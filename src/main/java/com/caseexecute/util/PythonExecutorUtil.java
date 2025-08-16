package com.caseexecute.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Python脚本执行工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
public class PythonExecutorUtil {
    
    /**
     * 执行Python脚本
     * 
     * @param scriptPath 脚本路径
     * @param testCaseId 用例ID
     * @param round 轮次
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public static PythonExecutionResult executePythonScript(Path scriptPath, Long testCaseId, Integer round) throws Exception {
        log.info("开始执行Python脚本 - 脚本路径: {}, 用例ID: {}, 轮次: {}", scriptPath, testCaseId, round);
        
        // 检查脚本文件是否存在
        if (!Files.exists(scriptPath)) {
            throw new RuntimeException("Python脚本文件不存在: " + scriptPath);
        }
        
        // 创建日志文件
        String logFileName = String.format("testcase_%d_round_%d_%s.log", 
                testCaseId, round, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Path logFilePath = Files.createTempFile("testcase_log_", ".log");
        
        LocalDateTime startTime = LocalDateTime.now();
        long startTimeMillis = System.currentTimeMillis();
        
        // 执行Python脚本
        ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath.toString());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFilePath.toFile());
        
        Process process = processBuilder.start();
        
        // 等待执行完成，设置超时时间（5分钟）
        boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
        
        LocalDateTime endTime = LocalDateTime.now();
        long endTimeMillis = System.currentTimeMillis();
        long executionTime = endTimeMillis - startTimeMillis;
        
        // 读取执行日志
        String logContent = new String(Files.readAllBytes(logFilePath), StandardCharsets.UTF_8);
        
        // 判断执行结果
        String status = "SUCCESS";
        String result = "用例执行成功";
        
        if (!completed) {
            status = "TIMEOUT";
            result = "用例执行超时";
            process.destroyForcibly();
        } else if (process.exitValue() != 0) {
            status = "FAILED";
            result = "用例执行失败，退出码: " + process.exitValue();
        } else {
            // 根据控制台输出判断结果
            if (logContent.contains("FAIL") || logContent.contains("ERROR") || logContent.contains("失败")) {
                status = "FAILED";
                result = "用例执行失败";
            } else if (logContent.contains("PASS") || logContent.contains("SUCCESS") || logContent.contains("成功")) {
                status = "SUCCESS";
                result = "用例执行成功";
            }
        }
        
        log.info("Python脚本执行完成 - 用例ID: {}, 轮次: {}, 状态: {}, 耗时: {}ms", 
                testCaseId, round, status, executionTime);
        
        // 清理日志文件
        Files.deleteIfExists(logFilePath);
        
        return PythonExecutionResult.builder()
                .status(status)
                .result(result)
                .executionTime(executionTime)
                .startTime(startTime)
                .endTime(endTime)
                .logContent(logContent)
                .logFileName(logFileName)
                .build();
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
        private String logFileName;
        
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
            
            public Builder logFileName(String logFileName) {
                executionResult.logFileName = logFileName;
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
        public String getLogFileName() { return logFileName; }
    }
}
