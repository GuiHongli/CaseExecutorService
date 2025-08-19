package com.caseexecute.util;

import com.caseexecute.config.FileStorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Python脚本执行工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class PythonExecutorUtil implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    private static FileStorageConfig fileStorageConfig;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        PythonExecutorUtil.applicationContext = applicationContext;
    }
    
    /**
     * 设置文件存储配置
     */
    public static void setFileStorageConfig(FileStorageConfig config) {
        PythonExecutorUtil.fileStorageConfig = config;
    }
    
    /**
     * 获取GoHttpServerClient实例
     */
    private static GoHttpServerClient getGoHttpServerClient() {
        if (applicationContext != null) {
            return applicationContext.getBean(GoHttpServerClient.class);
        }
        // 如果无法获取Spring bean，则创建新实例（不推荐，因为无法获取配置）
        log.warn("无法获取Spring ApplicationContext，创建新的GoHttpServerClient实例");
        return new GoHttpServerClient();
    }
    

    

    
    /**
     * 执行Python脚本（带超时参数）
     * 
     * @param scriptPath 脚本路径
     * @param testCaseId 用例ID
     * @param testCaseNumber 用例编号
     * @param round 轮次
     * @param timeoutMinutes 超时时间（分钟）
     * @param goHttpServerUrl gohttpserver地址（可选）
     * @param taskId 任务ID（可选）
     * @param executorIp 执行机IP地址
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public static PythonExecutionResult executePythonScript(Path scriptPath, Long testCaseId, String testCaseNumber, Integer round, Integer timeoutMinutes, String goHttpServerUrl, String taskId, String executorIp) throws Exception {
        log.info("开始执行Python脚本 - 脚本路径: {}, 用例ID: {}, 轮次: {}, 超时时间: {}分钟", 
                scriptPath, testCaseId, round, timeoutMinutes);
        
        // 检查脚本文件是否存在
        if (!Files.exists(scriptPath)) {
            throw new RuntimeException("Python脚本文件不存在: " + scriptPath);
        }
        
        // 创建日志目录 - 使用/opt/taskid/logs路径
        String rootDir = fileStorageConfig != null ? fileStorageConfig.getRootDirectory() : "/opt";
        Path rootDirectory = Paths.get(rootDir);
        Path taskDir = rootDirectory.resolve(taskId);
        Path logsDir = taskDir.resolve("logs");
        
        // 确保目录存在
        if (!Files.exists(rootDirectory)) {
            Files.createDirectories(rootDirectory);
        }
        if (!Files.exists(taskDir)) {
            Files.createDirectories(taskDir);
        }
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir);
        }
        
        // 创建日志文件路径 - 使用用例编号_轮次.log格式
        String logFileName;
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            logFileName = String.format("%s_%d.log", testCaseNumber, round);
        } else {
            // 如果没有用例编号，则使用用例ID
            logFileName = String.format("%d_%d.log", testCaseId, round);
        }
        Path logFilePath = logsDir.resolve(logFileName);
        
        LocalDateTime startTime = LocalDateTime.now();
        long startTimeMillis = System.currentTimeMillis();
        
        // 执行Python脚本 - 直接使用系统python3命令
        String pythonCommand = "python3";
        log.info("使用系统Python3执行器: {}", pythonCommand);
        
        log.info("执行机IP地址: {}", executorIp);
        
        // 构建命令参数：python3 script_path executor_ip
        ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, scriptPath.toString(), executorIp);
        processBuilder.redirectErrorStream(true);
        
        // 设置环境变量以确保正确的字符编码
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        processBuilder.environment().put("LANG", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_ALL", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_CTYPE", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_MESSAGES", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_MONETARY", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_NUMERIC", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_TIME", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_COLLATE", "zh_CN.UTF-8");
        
        Process process = processBuilder.start();
        
        // 声明completed变量
        boolean completed;
        
        // 输出执行开始日志
        RealTimeLogOutput.logExecutionStart(testCaseId, round, testCaseNumber, scriptPath.toString());
        
        // 创建日志文件输出流
        try (BufferedWriter logWriter = Files.newBufferedWriter(logFilePath, StandardCharsets.UTF_8)) {
            
            // 启动实时日志输出
            Future<?> logOutputFuture = RealTimeLogOutput.startRealTimeLogOutput(
                    process, logWriter, testCaseId, round, testCaseNumber);
            
            // 等待执行完成，使用配置的超时时间
            completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            
            // 等待日志输出完成
            RealTimeLogOutput.waitForLogOutput(logOutputFuture, 5);
        } // 关闭日志文件输出流
        
        LocalDateTime endTime = LocalDateTime.now();
        long endTimeMillis = System.currentTimeMillis();
        long executionTime = endTimeMillis - startTimeMillis;
        
        // 读取执行日志
        String logContent = new String(Files.readAllBytes(logFilePath), StandardCharsets.UTF_8);
        
        // 判断执行结果
        String status = "SUCCESS";
        String result = "用例执行成功";
        String failureReason = null;
        
        if (!completed) {
            status = "FAILED";  // 超时归类为FAILED
            result = "用例执行超时";
            failureReason = "用例执行超时: 超过配置的超时时间 " + timeoutMinutes + " 分钟";
            
            // 强制终止进程及其子进程
            terminateProcessAndChildren(process);
            
            RealTimeLogOutput.logError(testCaseId, round, testCaseNumber, 
                    "用例执行超时: 超过配置的超时时间 " + timeoutMinutes + " 分钟");
        } else if (process.exitValue() != 0) {
            // 检查是否是环境问题导致的阻塞
            if (isBlockedByEnvironment(logContent)) {
                status = "BLOCKED";
                result = "用例执行被阻塞（环境问题）";
                failureReason = "环境问题导致用例无法执行: " + analyzeFailureReason(logContent, process.exitValue());
            } else {
                status = "FAILED";
                result = "用例执行失败，退出码: " + process.exitValue();
                failureReason = analyzeFailureReason(logContent, process.exitValue());
            }
        } else {
            // 根据控制台输出判断结果
            TestResultAnalysis analysis = analyzeTestOutput(logContent);
            status = analysis.getStatus();
            result = analysis.getResult();
            failureReason = analysis.getFailureReason();
        }
        
        // 输出执行结束日志
        RealTimeLogOutput.logExecutionEnd(testCaseId, round, testCaseNumber, status, executionTime);
        
        // 上传日志文件到gohttpserver（如果提供了gohttpserver地址）
        String uploadedLogUrl = null;
        if (goHttpServerUrl != null && !goHttpServerUrl.trim().isEmpty()) {
            try {
                GoHttpServerClient goHttpServerClient = new GoHttpServerClient();
                uploadedLogUrl = goHttpServerClient.uploadLocalFile(logFilePath.toString(), logFileName, goHttpServerUrl);
                log.info("日志文件上传成功 - 用例ID: {}, 轮次: {}, 上传URL: {}", testCaseId, round, uploadedLogUrl);
            } catch (Exception e) {
                RealTimeLogOutput.logError(testCaseId, round, testCaseNumber, 
                        "日志文件上传失败: " + e.getMessage());
            }
        } else {
            log.info("未提供gohttpserver地址，跳过日志文件上传 - 用例ID: {}, 轮次: {}", testCaseId, round);
        }
        
        return PythonExecutionResult.builder()
                .status(status)
                .result(result)
                .executionTime(executionTime)
                .startTime(startTime)
                .endTime(endTime)
                .logContent(logContent)
                .logFilePath(uploadedLogUrl != null ? uploadedLogUrl : logFileName)
                .failureReason(failureReason)
                .build();
    }
    
    /**
     * 启动Python脚本进程（不等待完成）
     * 
     * @param scriptPath 脚本路径
     * @param testCaseId 用例ID
     * @param testCaseNumber 用例编号
     * @param round 轮次
     * @param goHttpServerUrl gohttpserver地址（可选）
     * @param taskId 任务ID（可选）
     * @param executorIp 执行机IP地址
     * @return 进程对象
     */
    public static Process startPythonProcess(Path scriptPath, Long testCaseId, String testCaseNumber, Integer round, String goHttpServerUrl, String taskId, String executorIp) throws Exception {
        log.info("启动Python脚本进程 - 脚本路径: {}, 用例ID: {}, 轮次: {}", 
                scriptPath, testCaseId, round);
        
        // 检查脚本文件是否存在
        if (!Files.exists(scriptPath)) {
            throw new RuntimeException("Python脚本文件不存在: " + scriptPath);
        }
        
        // 创建日志目录 - 使用/opt/taskid/logs路径
        String rootDir = fileStorageConfig != null ? fileStorageConfig.getRootDirectory() : "/opt";
        Path rootDirectory = Paths.get(rootDir);
        Path taskDir = rootDirectory.resolve(taskId);
        Path logsDir = taskDir.resolve("logs");
        
        // 确保目录存在
        if (!Files.exists(rootDirectory)) {
            Files.createDirectories(rootDirectory);
        }
        if (!Files.exists(taskDir)) {
            Files.createDirectories(taskDir);
        }
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir);
        }
        
        // 创建日志文件路径 - 使用用例编号_轮次.log格式
        String logFileName;
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            logFileName = String.format("%s_%d.log", testCaseNumber, round);
        } else {
            // 如果没有用例编号，则使用用例ID
            logFileName = String.format("%d_%d.log", testCaseId, round);
        }
        Path logFilePath = logsDir.resolve(logFileName);
        
        // 执行Python脚本 - 直接使用系统python3命令
        String pythonCommand = "python3";
        log.info("使用系统Python3执行器: {}", pythonCommand);
        
        log.info("执行机IP地址: {}", executorIp);
        
        // 构建命令参数：python3 script_path executor_ip
        ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, scriptPath.toString(), executorIp);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFilePath.toFile());
        
        // 设置环境变量以确保正确的字符编码
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        processBuilder.environment().put("LANG", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_ALL", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_CTYPE", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_MESSAGES", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_MONETARY", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_NUMERIC", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_TIME", "zh_CN.UTF-8");
        processBuilder.environment().put("LC_COLLATE", "zh_CN.UTF-8");
        
        Process process = processBuilder.start();
        log.info("Python脚本进程已启动 - 用例ID: {}, 轮次: {}, 日志文件: {}", 
                testCaseId, round, logFilePath);
        
        return process;
    }
    
    /**
     * 强制终止进程及其子进程
     * 
     * @param process 要终止的进程
     */
    private static void terminateProcessAndChildren(Process process) {
        if (process == null) {
            return;
        }
        
        try {
            log.info("开始终止进程及其子进程");
            
            // 在macOS/Linux系统上，使用pkill命令终止进程树
            if (System.getProperty("os.name").toLowerCase().contains("mac") || 
                System.getProperty("os.name").toLowerCase().contains("linux")) {
                terminateProcessTreeUnix(process);
            } else {
                // Windows系统或其他系统，使用Java API
                terminateProcessJava(process);
            }
            
        } catch (Exception e) {
            log.error("强制终止进程失败: {}", e.getMessage());
            // 降级到Java API
            terminateProcessJava(process);
        }
    }
    
    /**
     * 在Unix系统上终止进程树
     * 
     * @param process 进程对象
     */
    private static void terminateProcessTreeUnix(Process process) {
        try {
            // 在Java 8中，我们无法直接获取进程ID，所以直接使用Java API终止进程
            terminateProcessJava(process);
            log.info("已终止进程树");
        } catch (Exception e) {
            log.error("终止进程树失败 - 错误: {}", e.getMessage());
        }
    }
    
    /**
     * 使用Java API终止进程
     * 
     * @param process 要终止的进程
     */
    private static void terminateProcessJava(Process process) {
        try {
            // 首先尝试优雅终止
            process.destroy();
            
            // 等待5秒
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                // 如果还在运行，强制终止
                process.destroyForcibly();
                
                // 再等待5秒
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("进程可能仍在运行，无法完全终止");
                }
            }
            
            log.info("已终止进程");
        } catch (Exception e) {
            log.error("使用Java API终止进程失败: {}", e.getMessage());
        }
    }
    
    /**
     * 分析失败原因
     * 
     * @param logContent 日志内容
     * @param exitCode 进程退出码
     * @return 失败原因
     */
    private static String analyzeFailureReason(String logContent, int exitCode) {
        // 检查具体的环境问题
        if (logContent.contains("ImportError") || logContent.contains("ModuleNotFoundError")) {
            return "Python模块导入失败: 缺少必要的依赖包，请检查Python环境和依赖安装";
        } else if (logContent.contains("Permission denied") || logContent.contains("access denied")) {
            return "权限不足: 无法访问文件或目录，请检查文件权限设置";
        } else if (logContent.contains("No such file") || logContent.contains("file not found")) {
            return "文件不存在: 无法找到所需的文件或目录，请检查文件路径";
        } else if (logContent.contains("Connection refused") || logContent.contains("network is unreachable")) {
            return "网络连接失败: 无法连接到目标服务器，请检查网络配置";
        } else if (logContent.contains("Connection timeout") || logContent.contains("dns resolution failed")) {
            return "网络超时: DNS解析失败或连接超时，请检查网络连接";
        } else if (logContent.contains("out of memory") || logContent.contains("memory error")) {
            return "内存不足: 系统内存不足，无法执行用例";
        } else if (logContent.contains("disk space") || logContent.contains("no space left")) {
            return "磁盘空间不足: 系统磁盘空间不足，无法写入文件";
        } else if (logContent.contains("FAIL") || logContent.contains("ERROR") || logContent.contains("失败")) {
            return "用例执行失败: 日志中包含FAIL、ERROR或失败信息";
        } else if (logContent.contains("PASS") || logContent.contains("SUCCESS") || logContent.contains("成功")) {
            return "用例执行成功，但退出码非0: 可能存在其他问题，退出码: " + exitCode;
        } else {
            return "用例执行被阻塞: 未知原因导致执行失败，退出码: " + exitCode;
        }
    }
    
    /**
     * 分析测试输出结果
     * 
     * @param logContent 日志内容
     * @return 测试结果分析
     */
    private static TestResultAnalysis analyzeTestOutput(String logContent) {
        String status = "SUCCESS";
        String result = "用例执行成功";
        String failureReason = null;

        if (logContent.contains("FAIL") || logContent.contains("ERROR") || logContent.contains("失败")) {
            status = "FAILED";
            result = "用例执行失败";
            failureReason = "用例执行失败，日志中包含FAIL、ERROR或失败";
        } else if (logContent.contains("PASS") || logContent.contains("SUCCESS") || logContent.contains("成功")) {
            status = "SUCCESS";
            result = "用例执行成功";
            failureReason = null; // 成功时没有失败原因
        } else {
            status = "BLOCKED";
            result = "用例执行被阻塞";
            failureReason = "用例执行被阻塞: 日志中未包含明确的成功或失败标识，可能由于环境问题或脚本异常导致";
        }
        return new TestResultAnalysis(status, result, failureReason);
    }

    /**
     * Python执行结果
     */
    public static class PythonExecutionResult {
        private String status;
        private String result;
        private Long executionTime;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String logContent;
        private String logFilePath;
        private String failureReason;
        
        // 使用Builder模式
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private PythonExecutionResult executionResult = new PythonExecutionResult();
            
            public Builder status(String status) {
                executionResult.status = status;
                return this;
            }
            
            public Builder result(String resultValue) {
                executionResult.result = resultValue;
                return this;
            }
            
            public Builder executionTime(Long executionTime) {
                executionResult.executionTime = executionTime;
                return this;
            }
            
            public Builder startTime(LocalDateTime startTime) {
                executionResult.startTime = startTime;
                return this;
            }
            
            public Builder endTime(LocalDateTime endTime) {
                executionResult.endTime = endTime;
                return this;
            }
            
            public Builder logContent(String logContent) {
                executionResult.logContent = logContent;
                return this;
            }
            
            public Builder logFilePath(String logFilePath) {
                executionResult.logFilePath = logFilePath;
                return this;
            }

            public Builder failureReason(String failureReason) {
                executionResult.failureReason = failureReason;
                return this;
            }
            
            public PythonExecutionResult build() {
                return executionResult;
            }
        }
        
        // Getters
        public String getStatus() { return status; }
        public String getResult() { return result; }
        public Long getExecutionTime() { return executionTime; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getLogContent() { return logContent; }
        public String getLogFilePath() { return logFilePath; }
        public String getFailureReason() { return failureReason; }
    }

    /**
     * 检查是否因环境问题被阻塞
     * 
     * @param logContent 日志内容
     * @return 是否被阻塞
     */
    private static boolean isBlockedByEnvironment(String logContent) {
        if (logContent == null) {
            return false;
        }
        
        String lowerLog = logContent.toLowerCase();
        
        // 检查Python环境问题
        if (lowerLog.contains("importerror") || lowerLog.contains("modulenotfounderror") ||
            lowerLog.contains("no module named") || lowerLog.contains("python executable")) {
            return true;
        }
        
        // 检查文件权限问题
        if (lowerLog.contains("permission denied") || lowerLog.contains("access denied")) {
            return true;
        }
        
        // 检查文件不存在
        if (lowerLog.contains("no such file") || lowerLog.contains("file not found")) {
            return true;
        }
        
        // 检查网络连接问题
        if (lowerLog.contains("connection refused") || lowerLog.contains("network is unreachable") ||
            lowerLog.contains("connection timeout") || lowerLog.contains("dns resolution failed")) {
            return true;
        }
        
        // 检查系统资源问题
        if (lowerLog.contains("out of memory") || lowerLog.contains("disk space") ||
            lowerLog.contains("resource temporarily unavailable")) {
            return true;
        }
        
        return false;
    }

    /**
     * 测试结果分析
     */
    public static class TestResultAnalysis {
        private String status;
        private String result;
        private String failureReason;

        public TestResultAnalysis(String status, String result, String failureReason) {
            this.status = status;
            this.result = result;
            this.failureReason = failureReason;
        }

        public String getStatus() { return status; }
        public String getResult() { return result; }
        public String getFailureReason() { return failureReason; }
    }
}
