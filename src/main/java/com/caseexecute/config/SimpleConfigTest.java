package com.caseexecute.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 简单配置测试，使用@Value注解直接注入配置值
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class SimpleConfigTest implements CommandLineRunner {
    
    @Value("${case-execution.file-storage.root-directory:/tmp/default}")
    private String rootDirectory;
    
    @Value("${case-execution.file-storage.auto-cleanup:true}")
    private boolean autoCleanup;
    
    @Value("${case-execution.file-storage.retention-hours:24}")
    private int retentionHours;
    
    @Value("${case-execution.file-storage.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;
    
    @Value("${case-execution.file-storage.max-file-size-mb:1000}")
    private long maxFileSizeMB;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 简单配置测试开始 ===");
        log.info("使用@Value注入的配置值:");
        log.info("rootDirectory: {}", rootDirectory);
        log.info("autoCleanup: {}", autoCleanup);
        log.info("retentionHours: {}", retentionHours);
        log.info("maxConcurrentTasks: {}", maxConcurrentTasks);
        log.info("maxFileSizeMB: {}", maxFileSizeMB);
        log.info("=== 简单配置测试结束 ===");
    }
}
