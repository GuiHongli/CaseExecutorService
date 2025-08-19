package com.caseexecute.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 实时日志输出工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
public class RealTimeLogOutput {
    
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    /**
     * 实时输出进程日志
     * 
     * @param process 进程对象
     * @param logWriter 日志文件写入器
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param testCaseNumber 用例编号
     * @return 日志输出Future
     */
    public static Future<?> startRealTimeLogOutput(Process process, BufferedWriter logWriter, 
                                                  Long testCaseId, Integer round, String testCaseNumber) {
        return executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    // 获取当前时间
                    String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
                    
                    // 构建日志前缀
                    String logPrefix = String.format("[%s] [用例执行] 用例ID: %d, 轮次: %d", 
                            timestamp, testCaseId, round);
                    
                    if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
                        logPrefix += String.format(", 用例编号: %s", testCaseNumber);
                    }
                    
                    // 实时输出到控制台
                    log.info("{} - {}", logPrefix, line);
                    
                    // 同时写入日志文件
                    synchronized (logWriter) {
                        logWriter.write(String.format("[%s] %s", timestamp, line));
                        logWriter.newLine();
                        logWriter.flush();
                    }
                }
            } catch (IOException e) {
                log.error("读取进程输出流失败 - 用例ID: {}, 轮次: {}, 错误: {}", 
                        testCaseId, round, e.getMessage());
            }
        });
    }
    
    /**
     * 等待日志输出完成
     * 
     * @param logOutputFuture 日志输出Future
     * @param timeoutSeconds 超时时间（秒）
     */
    public static void waitForLogOutput(Future<?> logOutputFuture, int timeoutSeconds) {
        try {
            logOutputFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("等待日志输出完成时超时或出错: {}", e.getMessage());
        }
    }
    
    /**
     * 输出执行开始日志
     * 
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param testCaseNumber 用例编号
     * @param scriptPath 脚本路径
     */
    public static void logExecutionStart(Long testCaseId, Integer round, String testCaseNumber, String scriptPath) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logPrefix = String.format("[%s] [用例执行] 用例ID: %d, 轮次: %d", timestamp, testCaseId, round);
        
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            logPrefix += String.format(", 用例编号: %s", testCaseNumber);
        }
        
        log.info("{} - 开始执行Python脚本: {}", logPrefix, scriptPath);
    }
    
    /**
     * 输出执行结束日志
     * 
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param testCaseNumber 用例编号
     * @param status 执行状态
     * @param executionTime 执行时间（毫秒）
     */
    public static void logExecutionEnd(Long testCaseId, Integer round, String testCaseNumber, 
                                     String status, long executionTime) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logPrefix = String.format("[%s] [用例执行] 用例ID: %d, 轮次: %d", timestamp, testCaseId, round);
        
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            logPrefix += String.format(", 用例编号: %s", testCaseNumber);
        }
        
        log.info("{} - 执行完成, 状态: {}, 耗时: {}ms", logPrefix, status, executionTime);
    }
    
    /**
     * 输出错误日志
     * 
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param testCaseNumber 用例编号
     * @param errorMessage 错误信息
     */
    public static void logError(Long testCaseId, Integer round, String testCaseNumber, String errorMessage) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logPrefix = String.format("[%s] [用例执行] 用例ID: %d, 轮次: %d", timestamp, testCaseId, round);
        
        if (testCaseNumber != null && !testCaseNumber.trim().isEmpty()) {
            logPrefix += String.format(", 用例编号: %s", testCaseNumber);
        }
        
        log.error("{} - 执行错误: {}", logPrefix, errorMessage);
    }
}
