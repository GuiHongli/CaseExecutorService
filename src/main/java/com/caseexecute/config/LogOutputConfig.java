package com.caseexecute.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志输出配置类
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "case-execution.log")
public class LogOutputConfig {
    
    /**
     * 是否启用实时日志输出到控制台
     */
    private boolean enableRealTimeConsoleOutput = true;
    
    /**
     * 是否启用日志文件输出
     */
    private boolean enableFileOutput = true;
    
    /**
     * 日志输出级别过滤（INFO, DEBUG, WARN, ERROR）
     */
    private String logLevel = "INFO";
    
    /**
     * 是否显示时间戳
     */
    private boolean showTimestamp = true;
    
    /**
     * 是否显示用例信息前缀
     */
    private boolean showTestCasePrefix = true;
    
    /**
     * 日志输出格式
     */
    private String logFormat = "[{timestamp}] [用例执行] 用例ID: {testCaseId}, 轮次: {round} - {message}";
}
