package com.caseexecute.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置类
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "case-execution.file-storage")
public class FileStorageConfig {
    
    /**
     * 文件存储根目录
     */
    private String rootDirectory = "/opt";
    
    /**
     * 是否在任务完成后自动清理文件
     */
    private boolean autoCleanup = true;
    
    /**
     * 文件保留时间（小时），超过此时间自动清理
     */
    private int retentionHours = 24;
    
    /**
     * 最大并发任务数
     */
    private int maxConcurrentTasks = 10;
    
    /**
     * 单个任务最大文件大小（MB）
     */
    private long maxFileSizeMB = 1000;
}
