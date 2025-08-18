package com.caseexecute.service.impl;

import com.caseexecute.config.CaseExecutionConfig;
import com.caseexecute.dto.TestCaseExecutionRequest;
import com.caseexecute.dto.TestCaseResultReport;
import com.caseexecute.service.TestCaseExecutionService;
import com.caseexecute.util.FileDownloadUtil;
import com.caseexecute.util.HttpReportUtil;
import com.caseexecute.util.PythonExecutorUtil;
import com.caseexecute.util.TestCaseResultParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用例执行服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TestCaseExecutionServiceImpl implements TestCaseExecutionService {

    @Autowired
    private CaseExecutionConfig caseExecutionConfig;
    
    @Autowired
    private HttpReportUtil httpReportUtil;
    
    // 任务管理：存储正在执行的任务和进程信息
    private final Map<String, TaskExecutionInfo> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 任务执行信息
     */
    private static class TaskExecutionInfo {
        private final String taskId;
        private final List<Process> processes;
        private final CompletableFuture<Void> executionFuture;
        private final LocalDateTime startTime;
        
        public TaskExecutionInfo(String taskId, CompletableFuture<Void> executionFuture) {
            this.taskId = taskId;
            this.processes = new ArrayList<>();
            this.executionFuture = executionFuture;
            this.startTime = LocalDateTime.now();
        }
        
        public void addProcess(Process process) {
            processes.add(process);
        }
        
        public void removeProcess(Process process) {
            processes.remove(process);
        }
        
        public void cancelAllProcesses() {
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    try {
                        // 直接调用进程的destroy方法
                        process.destroy();
                        if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                            process.destroyForcibly();
                        }
                        log.info("已终止进程 - 任务ID: {}, PID: {}", taskId, process.pid());
                    } catch (Exception e) {
                        log.error("终止进程失败 - 任务ID: {}, PID: {}, 错误: {}", taskId, process.pid(), e.getMessage());
                    }
                }
            }
            processes.clear();
        }
        
        public void cancelExecution() {
            if (executionFuture != null && !executionFuture.isDone()) {
                executionFuture.cancel(true);
                log.info("已取消任务执行 - 任务ID: {}", taskId);
            }
        }
        
        // Getters
        public String getTaskId() { return taskId; }
        public List<Process> getProcesses() { return processes; }
        public CompletableFuture<Void> getExecutionFuture() { return executionFuture; }
        public LocalDateTime getStartTime() { return startTime; }
    }

    @Override
    public void processTestCaseExecution(TestCaseExecutionRequest request) {
        log.info("开始处理用例执行任务 - 任务ID: {}", request.getTaskId());
        
        // 异步执行，避免阻塞接口响应
        CompletableFuture<Void> executionFuture = CompletableFuture.runAsync(() -> {
            Path zipFilePath = null;
            Path extractPath = null;
            
            try {
                log.info("开始下载用例集文件 - 任务ID: {}, URL: {}", request.getTaskId(), request.getTestCaseSetPath());
                
                // 1. 下载用例集文件
                zipFilePath = FileDownloadUtil.downloadFile(request.getTestCaseSetPath());
                log.info("用例集文件下载完成 - 任务ID: {}, 文件路径: {}", request.getTaskId(), zipFilePath);
                
                // 2. 解压用例集到临时目录
                log.info("开始解压用例集文件 - 任务ID: {}, 文件路径: {}", request.getTaskId(), zipFilePath);
                extractPath = FileDownloadUtil.extractZipFile(zipFilePath);
                log.info("用例集文件解压完成 - 任务ID: {}, 解压路径: {}", request.getTaskId(), extractPath);
                
                // 3. 执行用例列表
                log.info("开始执行用例列表 - 任务ID: {}, 用例数量: {}", request.getTaskId(), request.getTestCaseList().size());
                executeTestCaseList(request, extractPath);
                
                log.info("用例执行任务处理完成 - 任务ID: {}", request.getTaskId());
                
            } catch (Exception e) {
                log.error("用例执行任务处理失败 - 任务ID: {}, 错误: {}", request.getTaskId(), e.getMessage(), e);
            } finally {
                // 4. 清理临时文件
                log.info("开始清理临时文件 - 任务ID: {}", request.getTaskId());
                if (zipFilePath != null) {
                    FileDownloadUtil.cleanupFile(zipFilePath);
                    log.info("临时ZIP文件已清理 - 任务ID: {}, 文件路径: {}", request.getTaskId(), zipFilePath);
                }
                if (extractPath != null) {
                    FileDownloadUtil.cleanupFile(extractPath);
                    log.info("临时解压目录已清理 - 任务ID: {}, 目录路径: {}", request.getTaskId(), extractPath);
                }
                log.info("临时文件清理完成 - 任务ID: {}", request.getTaskId());
                
                // 5. 从运行任务列表中移除
                runningTasks.remove(request.getTaskId());
                log.info("任务已从运行列表中移除 - 任务ID: {}", request.getTaskId());
            }
        });
        
        // 创建任务执行信息并存储
        TaskExecutionInfo taskInfo = new TaskExecutionInfo(request.getTaskId(), executionFuture);
        runningTasks.put(request.getTaskId(), taskInfo);
        log.info("任务已添加到运行列表 - 任务ID: {}", request.getTaskId());
    }
    
    /**
     * 执行用例列表
     */
    private void executeTestCaseList(TestCaseExecutionRequest request, Path extractPath) {
        log.info("开始执行用例列表 - 用例数量: {}", request.getTestCaseList().size());
        
        int successCount = 0;
        int failedCount = 0;
        
        for (TestCaseExecutionRequest.TestCaseInfo testCase : request.getTestCaseList()) {
            try {
                log.info("开始执行用例 - 用例ID: {}, 用例编号: {}, 轮次: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound());
                
                executeSingleTestCase(request, testCase, extractPath);
                successCount++;
                
                log.info("用例执行完成 - 用例ID: {}, 用例编号: {}, 轮次: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound());
                
            } catch (Exception e) {
                failedCount++;
                log.error("执行用例异常 - 用例ID: {}, 用例编号: {}, 轮次: {}, 错误: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), e.getMessage(), e);
                
                // 对于未处理的异常，上报为FAILED状态
                try {
                    reportTestCaseResult(request, testCase, "FAILED", "执行异常: " + e.getMessage(), 0L, null, null, "执行异常: " + e.getMessage());
                } catch (Exception reportException) {
                    log.error("上报用例执行结果失败 - 用例ID: {}, 错误: {}", testCase.getTestCaseId(), reportException.getMessage());
                }
            }
        }
        
        log.info("用例列表执行完成 - 成功: {}, 失败: {}, 总计: {}", 
                successCount, failedCount, request.getTestCaseList().size());
    }
    
    /**
     * 查找脚本文件
     */
    private Path findScriptFile(Path extractPath, TestCaseExecutionRequest.TestCaseInfo testCase) {
        String testCaseNumber = testCase.getTestCaseNumber();
        Long testCaseId = testCase.getTestCaseId();
        
        // 1. 优先尝试精确匹配用例编号
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            Path scriptPath = extractPath.resolve("scripts").resolve(testCaseNumber + ".py");
            if (java.nio.file.Files.exists(scriptPath)) {
                log.info("找到精确匹配的脚本文件: {}", scriptPath);
                return scriptPath;
            }
            
            // 尝试cases目录
            scriptPath = extractPath.resolve("cases").resolve(testCaseNumber + ".py");
            if (java.nio.file.Files.exists(scriptPath)) {
                log.info("找到精确匹配的脚本文件: {}", scriptPath);
                return scriptPath;
            }
        }
        
        // 2. 尝试用例ID
        Path scriptPath = extractPath.resolve("scripts").resolve(testCaseId + ".py");
        if (java.nio.file.Files.exists(scriptPath)) {
            log.info("找到用例ID匹配的脚本文件: {}", scriptPath);
            return scriptPath;
        }
        
        scriptPath = extractPath.resolve("cases").resolve(testCaseId + ".py");
        if (java.nio.file.Files.exists(scriptPath)) {
            log.info("找到用例ID匹配的脚本文件: {}", scriptPath);
            return scriptPath;
        }
        
        // 3. 智能匹配：根据用例编号映射到可能的脚本文件名
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            String[] possibleScriptNames = getPossibleScriptNames(testCaseNumber);
            for (String scriptName : possibleScriptNames) {
                scriptPath = extractPath.resolve("scripts").resolve(scriptName);
                if (java.nio.file.Files.exists(scriptPath)) {
                    log.info("找到智能匹配的脚本文件: {} -> {}", testCaseNumber, scriptPath);
                    return scriptPath;
                }
                
                scriptPath = extractPath.resolve("cases").resolve(scriptName);
                if (java.nio.file.Files.exists(scriptPath)) {
                    log.info("找到智能匹配的脚本文件: {} -> {}", testCaseNumber, scriptPath);
                    return scriptPath;
                }
            }
        }
        
        // 4. 如果都找不到，返回第一个可用的Python脚本
        try {
            java.nio.file.Files.walk(extractPath)
                .filter(path -> path.toString().endsWith(".py"))
                .findFirst()
                .ifPresent(path -> {
                    log.warn("未找到匹配的脚本文件，使用第一个可用的Python脚本: {} -> {}", testCaseNumber, path);
                });
        } catch (Exception e) {
            log.error("搜索Python脚本文件时出错: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 根据用例编号获取可能的脚本文件名
     */
    private String[] getPossibleScriptNames(String testCaseNumber) {
        // 这里可以根据实际的命名规则进行映射
        // 例如：TC001 -> 4G_Network_Connection_Test.py
        switch (testCaseNumber) {
            case "TC001":
                return new String[]{"4G_Network_Connection_Test.py", "test_network_connection.py"};
            case "TC002":
                return new String[]{"5G_Network_Performance_Test.py", "test_network_performance.py"};
            case "TC003":
                return new String[]{"WiFi_Connection_Stability_Test.py", "test_wifi_stability.py"};
            case "TC004":
                return new String[]{"Multi_Network_Switch_Test.py", "test_network_switch.py"};
            case "TC005":
                return new String[]{"Weak_Network_Test.py", "test_weak_network.py"};
            case "TC006":
                return new String[]{"Network_Interruption_Recovery_Test.py", "test_network_recovery.py"};
            case "TC007":
                return new String[]{"High_Concurrency_Network_Test.py", "test_high_concurrency.py"};
            case "TC008":
                return new String[]{"Network_Security_Test.py", "test_network_security.py"};
            default:
                return new String[]{};
        }
    }
    
    /**
     * 执行单个用例
     */
    private void executeSingleTestCase(TestCaseExecutionRequest request, 
                                     TestCaseExecutionRequest.TestCaseInfo testCase, 
                                     Path extractPath) throws Exception {
        log.info("开始执行用例 - 用例ID: {}, 用例编号: {}, 轮次: {}", 
                testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound());
        
        // 查找用例脚本文件 - 在scripts目录中查找，只使用用例编号
        if (testCase.getTestCaseNumber() == null || testCase.getTestCaseNumber().trim().isEmpty()) {
            String failureReason = "用例编号为空，无法查找脚本文件";
            log.error("用例编号为空 - 用例ID: {}, 轮次: {}", testCase.getTestCaseId(), testCase.getRound());
            reportTestCaseResult(request, testCase, "BLOCKED", "用例执行失败", 0L, null, null, failureReason);
            return;
        }
        
        String scriptFileName = testCase.getTestCaseNumber() + ".py";
        Path scriptPath = extractPath.resolve("scripts").resolve(scriptFileName);
        
        if (!java.nio.file.Files.exists(scriptPath)) {
            String failureReason = "Python脚本文件不存在: scripts/" + scriptFileName + " (用例编号: " + testCase.getTestCaseNumber() + ")";
            
            log.error("脚本文件不存在 - 用例ID: {}, 用例编号: {}, 轮次: {}, 错误: {}", 
                    testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), failureReason);
            
            // 上报Blocked状态和失败原因
            reportTestCaseResult(request, testCase, "BLOCKED", "用例执行失败", 0L, null, null, failureReason);
            return;
        }
        
        try {
            // 执行Python脚本，使用配置的超时时间
            Integer timeoutMinutes = caseExecutionConfig.getTimeoutMinutes();
            log.info("使用配置的超时时间执行用例 - 用例ID: {}, 用例编号: {}, 轮次: {}, 超时时间: {}分钟", 
                    testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), timeoutMinutes);
            
            PythonExecutorUtil.PythonExecutionResult executionResult = 
                    PythonExecutorUtil.executePythonScript(scriptPath, testCase.getTestCaseId(), testCase.getRound(), timeoutMinutes);
            
            // 解析执行结果和失败原因
            String status = executionResult.getStatus();
            String result = executionResult.getResult();
            String failureReason = executionResult.getFailureReason();
            
            // 根据执行结果进行详细分析
            TestCaseAnalysis analysis = analyzeTestCaseResult(executionResult, testCase);
            
            // 上报解析后的执行结果
            log.info("准备上报用例执行结果 - 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}", 
                    testCase.getTestCaseId(), testCase.getRound(), analysis.getStatus(), analysis.getResult(), analysis.getFailureReason());
            log.info("结果上报URL: {}", request.getResultReportUrl());
            
            reportTestCaseResult(request, testCase, analysis.getStatus(), analysis.getResult(), 
                    executionResult.getExecutionTime(), executionResult.getStartTime(), executionResult.getEndTime(), analysis.getFailureReason());
            
            // 上报执行日志
            log.info("准备上报用例执行日志 - 用例ID: {}, 轮次: {}, 日志文件名: {}, 日志内容长度: {}", 
                    testCase.getTestCaseId(), testCase.getRound(), executionResult.getLogFileName(), 
                    executionResult.getLogContent() != null ? executionResult.getLogContent().length() : 0);
            log.info("日志上报URL: {}", request.getLogReportUrl());
            
            httpReportUtil.reportTestCaseLog(request.getLogReportUrl(), executionResult.getLogContent(), 
                    executionResult.getLogFileName(), testCase.getTestCaseId(), testCase.getRound());
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String failureReason;
            
            // 检查是否是Python执行器不可用的错误
            if (errorMessage != null && errorMessage.contains("Cannot run program \"python\"") && errorMessage.contains("No such file or directory")) {
                failureReason = "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中";
                log.error("Python执行器不可用 - 用例ID: {}, 用例编号: {}, 轮次: {}, 错误: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), errorMessage);
            } else {
                failureReason = "Python脚本执行异常: " + errorMessage;
                log.error("Python脚本执行异常 - 用例ID: {}, 用例编号: {}, 轮次: {}, 错误: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), errorMessage);
            }
            
            // 上报BLOCKED状态和错误原因
            reportTestCaseResult(request, testCase, "BLOCKED", "用例执行失败", 0L, null, null, failureReason);
        }
    }
    
    /**
     * 分析用例执行结果
     * 
     * @param executionResult 执行结果
     * @param testCase 用例信息
     * @return 用例分析结果
     */
    private TestCaseAnalysis analyzeTestCaseResult(PythonExecutorUtil.PythonExecutionResult executionResult, 
                                                 TestCaseExecutionRequest.TestCaseInfo testCase) {
        String status = executionResult.getStatus();
        String result = executionResult.getResult();
        String failureReason = executionResult.getFailureReason();
        String logContent = executionResult.getLogContent();
        
        // 使用智能解析工具分析结果
        TestCaseResultParser.TestCaseParseResult parseResult = TestCaseResultParser.parseResult(logContent);
        
        // 如果解析工具能够识别出结果，使用解析结果
        if (!"BLOCKED".equals(parseResult.getStatus())) {
            status = parseResult.getStatus();
            result = parseResult.getResultMessage();
            
            // 构建详细的失败原因
            if (parseResult.getFailureDetails() != null) {
                failureReason = parseResult.getFailureDetails();
            } else if (parseResult.getFailedTests() > 0 || parseResult.getErrorTests() > 0) {
                failureReason = String.format("测试统计: 总测试数=%d, 成功=%d, 失败=%d, 错误=%d, 成功率=%.1f%%", 
                        parseResult.getTotalTests(), parseResult.getSuccessTests(), 
                        parseResult.getFailedTests(), parseResult.getErrorTests(), parseResult.getSuccessRate());
            }
        } else {
            // 降级到原有的分析逻辑
            if ("SUCCESS".equals(status)) {
                // 检查是否真的成功
                if (logContent.contains("FAIL") || logContent.contains("ERROR") || logContent.contains("失败")) {
                    status = "FAILED";
                    result = "用例执行失败";
                    failureReason = "日志分析发现失败信息: " + extractFailureDetails(logContent);
                } else if (logContent.contains("PASS") || logContent.contains("SUCCESS") || logContent.contains("成功")) {
                    status = "SUCCESS";
                    result = "用例执行成功";
                    failureReason = null;
                } else {
                    // 没有明确的成功或失败标识
                    status = "BLOCKED";
                    result = "用例执行被阻塞";
                    failureReason = "用例执行被阻塞: 日志中未包含明确的成功或失败标识，可能由于环境问题或脚本异常导致";
                }
            } else if ("FAILED".equals(status)) {
                // 分析失败原因
                failureReason = analyzeDetailedFailureReason(logContent, failureReason);
            } else if ("BLOCKED".equals(status)) {
                // 阻塞状态的处理 - 提供更具体的阻塞原因
                failureReason = analyzeDetailedFailureReason(logContent, failureReason);
            }
        }
        
        // 添加性能指标信息
        if (parseResult.getNetworkLatency() != null) {
            result += String.format(" (网络延迟: %.2fms)", parseResult.getNetworkLatency());
        }
        if (parseResult.getBandwidth() != null) {
            result += String.format(" (带宽: %.2f%s)", parseResult.getBandwidth(), parseResult.getBandwidthUnit());
        }
        if (parseResult.getSignalStrength() != null) {
            result += String.format(" (信号强度: %.2fdBm)", parseResult.getSignalStrength());
        }
        
        return new TestCaseAnalysis(status, result, failureReason);
    }
    
    /**
     * 提取失败详情
     * 
     * @param logContent 日志内容
     * @return 失败详情
     */
    private String extractFailureDetails(String logContent) {
        // 提取失败的具体信息
        String[] lines = logContent.split("\n");
        StringBuilder failureDetails = new StringBuilder();
        
        for (String line : lines) {
            if (line.contains("FAIL") || line.contains("ERROR") || line.contains("失败") || 
                line.contains("AssertionError") || line.contains("Exception")) {
                failureDetails.append(line.trim()).append("; ");
            }
        }
        
        return failureDetails.length() > 0 ? failureDetails.toString() : "阻塞失败原因";
    }
    
    /**
     * 分析详细失败原因
     * 
     * @param logContent 日志内容
     * @param originalReason 原始失败原因
     * @return 详细失败原因
     */
    private String analyzeDetailedFailureReason(String logContent, String originalReason) {
        if (logContent.contains("网络连接失败") || logContent.contains("Connection refused")) {
            return "网络连接失败: 无法连接到目标服务器，请检查网络配置和服务器状态";
        } else if (logContent.contains("超时") || logContent.contains("timeout")) {
            return "网络请求超时: 服务器响应时间过长，请检查网络连接和服务器负载";
        } else if (logContent.contains("DNS解析失败") || logContent.contains("Name or service not known")) {
            return "DNS解析失败: 无法解析域名，请检查DNS配置和网络连接";
        } else if (logContent.contains("权限不足") || logContent.contains("Permission denied")) {
            return "权限不足: 无法访问所需资源，请检查文件权限和用户权限设置";
        } else if (logContent.contains("文件不存在") || logContent.contains("No such file")) {
            return "文件不存在: 无法找到所需的文件或目录，请检查文件路径和文件是否存在";
        } else if (logContent.contains("模块导入失败") || logContent.contains("ImportError")) {
            return "模块导入失败: Python依赖包缺失，请检查Python环境和依赖包安装";
        } else if (logContent.contains("内存不足") || logContent.contains("out of memory")) {
            return "内存不足: 系统内存不足，无法执行用例，请检查系统资源";
        } else if (logContent.contains("磁盘空间不足") || logContent.contains("no space left")) {
            return "磁盘空间不足: 系统磁盘空间不足，无法写入文件，请清理磁盘空间";
        } else if (logContent.contains("Python执行器不可用")) {
            return "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中，请检查Python安装";
        } else {
            return originalReason != null ? originalReason : "用例执行被阻塞: 未知原因导致执行失败";
        }
    }
    
    /**
     * 用例分析结果
     */
    private static class TestCaseAnalysis {
        private String status;
        private String result;
        private String failureReason;
        
        public TestCaseAnalysis(String status, String result, String failureReason) {
            this.status = status;
            this.result = result;
            this.failureReason = failureReason;
        }
        
        public String getStatus() { return status; }
        public String getResult() { return result; }
        public String getFailureReason() { return failureReason; }
    }
    
    /**
     * 上报用例执行结果
     */
    private void reportTestCaseResult(TestCaseExecutionRequest request,
                                    TestCaseExecutionRequest.TestCaseInfo testCase,
                                    String status,
                                    String result,
                                    Long executionTime,
                                    java.time.LocalDateTime startTime,
                                    java.time.LocalDateTime endTime) {
        reportTestCaseResult(request, testCase, status, result, executionTime, startTime, endTime, null);
    }
    
    /**
     * 上报用例执行结果（带失败原因）
     */
    private void reportTestCaseResult(TestCaseExecutionRequest request,
                                    TestCaseExecutionRequest.TestCaseInfo testCase,
                                    String status,
                                    String result,
                                    Long executionTime,
                                    java.time.LocalDateTime startTime,
                                    java.time.LocalDateTime endTime,
                                    String failureReason) {
        log.info("构建用例执行结果报告 - 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                testCase.getTestCaseId(), testCase.getRound(), status, result);
        
        TestCaseResultReport report = new TestCaseResultReport();
        report.setTaskId(request.getTaskId());
        report.setTestCaseId(testCase.getTestCaseId());
        report.setRound(testCase.getRound());
        report.setStatus(status);
        report.setResult(result);
        report.setExecutionTime(executionTime);
        report.setStartTime(startTime);
        report.setEndTime(endTime);
        report.setExecutorIp(request.getExecutorIp());
        report.setTestCaseSetId(request.getTestCaseSetId());
        
        if ("FAILED".equals(status) || "BLOCKED".equals(status)) {
            report.setFailureReason(failureReason != null ? failureReason : result);
            log.info("设置失败原因 - 用例ID: {}, 轮次: {}, 失败原因: {}", 
                    testCase.getTestCaseId(), testCase.getRound(), report.getFailureReason());
        }
        
        log.info("用例执行结果报告构建完成 - 用例ID: {}, 轮次: {}, 任务ID: {}, 执行机IP: {}", 
                testCase.getTestCaseId(), testCase.getRound(), request.getTaskId(), request.getExecutorIp());
        
        httpReportUtil.reportTestCaseResult(request.getResultReportUrl(), report);
    }
    
    @Override
    public boolean cancelTaskExecution(String taskId) {
        log.info("开始取消任务执行 - 任务ID: {}", taskId);
        
        TaskExecutionInfo taskInfo = runningTasks.get(taskId);
        if (taskInfo == null) {
            log.warn("任务不存在或已完成 - 任务ID: {}", taskId);
            return false;
        }
        
        try {
            // 1. 取消所有相关进程
            taskInfo.cancelAllProcesses();
            
            // 2. 取消执行Future
            taskInfo.cancelExecution();
            
            // 3. 从运行任务列表中移除
            runningTasks.remove(taskId);
            
            log.info("任务取消成功 - 任务ID: {}", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("取消任务失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}
