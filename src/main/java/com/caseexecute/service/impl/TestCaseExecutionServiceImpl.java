package com.caseexecute.service.impl;

import com.caseexecute.dto.TestCaseExecutionRequest;
import com.caseexecute.dto.TestCaseResultReport;
import com.caseexecute.service.TestCaseExecutionService;
import com.caseexecute.util.FileDownloadUtil;
import com.caseexecute.util.HttpReportUtil;
import com.caseexecute.util.PythonExecutorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 用例执行服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TestCaseExecutionServiceImpl implements TestCaseExecutionService {

    @Override
    public void processTestCaseExecution(TestCaseExecutionRequest request) {
        log.info("开始处理用例执行任务 - 任务ID: {}", request.getTaskId());
        
        // 异步执行，避免阻塞接口响应
        CompletableFuture.runAsync(() -> {
            Path zipFilePath = null;
            Path extractPath = null;
            
            try {
                // 1. 下载用例集文件
                zipFilePath = FileDownloadUtil.downloadFile(request.getTestCaseSetPath());
                
                // 2. 解压用例集到临时目录
                extractPath = FileDownloadUtil.extractZipFile(zipFilePath);
                
                // 3. 执行用例列表
                executeTestCaseList(request, extractPath);
                
                log.info("用例执行任务处理完成 - 任务ID: {}", request.getTaskId());
                
            } catch (Exception e) {
                log.error("用例执行任务处理失败 - 任务ID: {}, 错误: {}", request.getTaskId(), e.getMessage(), e);
            } finally {
                // 4. 清理临时文件
                if (zipFilePath != null) {
                    FileDownloadUtil.cleanupFile(zipFilePath);
                }
                if (extractPath != null) {
                    FileDownloadUtil.cleanupFile(extractPath);
                }
            }
        });
    }
    
    /**
     * 执行用例列表
     */
    private void executeTestCaseList(TestCaseExecutionRequest request, Path extractPath) {
        log.info("开始执行用例列表 - 用例数量: {}", request.getTestCaseList().size());
        
        for (TestCaseExecutionRequest.TestCaseInfo testCase : request.getTestCaseList()) {
            try {
                executeSingleTestCase(request, testCase, extractPath);
            } catch (Exception e) {
                log.error("执行用例失败 - 用例ID: {}, 用例编号: {}, 轮次: {}, 错误: {}", 
                        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound(), e.getMessage(), e);
                
                // 上报失败结果
                reportTestCaseResult(request, testCase, "FAILED", e.getMessage(), 0L, null, null);
            }
        }
        
        log.info("用例列表执行完成");
    }
    
    /**
     * 执行单个用例
     */
    private void executeSingleTestCase(TestCaseExecutionRequest request, 
                                     TestCaseExecutionRequest.TestCaseInfo testCase, 
                                     Path extractPath) throws Exception {
        log.info("开始执行用例 - 用例ID: {}, 用例编号: {}, 轮次: {}", 
                testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound());
        
        // 查找用例脚本文件 - 优先使用用例编号，如果没有则使用用例ID
        String scriptFileName = testCase.getTestCaseNumber() != null && !testCase.getTestCaseNumber().trim().isEmpty() 
                ? testCase.getTestCaseNumber() + ".py" 
                : testCase.getTestCaseId() + ".py";
        Path scriptPath = extractPath.resolve("cases").resolve(scriptFileName);
        
        // 执行Python脚本
        PythonExecutorUtil.PythonExecutionResult executionResult = 
                PythonExecutorUtil.executePythonScript(scriptPath, testCase.getTestCaseId(), testCase.getRound());
        
        // 上报执行结果
        reportTestCaseResult(request, testCase, executionResult.getStatus(), executionResult.getResult(), 
                executionResult.getExecutionTime(), executionResult.getStartTime(), executionResult.getEndTime());
        
        // 上报执行日志
        HttpReportUtil.reportTestCaseLog(request.getLogReportUrl(), executionResult.getLogContent(), 
                executionResult.getLogFileName(), testCase.getTestCaseId(), testCase.getRound());
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
        
        if ("FAILED".equals(status)) {
            report.setErrorMessage(result);
        }
        
        HttpReportUtil.reportTestCaseResult(request.getResultReportUrl(), report);
    }
}
