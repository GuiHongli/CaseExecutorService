package com.caseexecute.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 配置测试组件，用于验证配置是否正确加载
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class ConfigTestComponent implements CommandLineRunner {
    
    @Autowired
    private FileStorageConfig fileStorageConfig;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 配置验证开始 ===");
        log.info("FileStorageConfig对象: {}", fileStorageConfig);
        
        if (fileStorageConfig != null) {
            log.info("根目录: {}", fileStorageConfig.getRootDirectory());
            log.info("自动清理: {}", fileStorageConfig.isAutoCleanup());
            log.info("保留时间: {}小时", fileStorageConfig.getRetentionHours());
            log.info("最大并发任务数: {}", fileStorageConfig.getMaxConcurrentTasks());
            log.info("最大文件大小: {}MB", fileStorageConfig.getMaxFileSizeMB());
            
            // 打印详细配置信息
            fileStorageConfig.printConfig();
        } else {
            log.error("FileStorageConfig对象为null，配置加载失败！");
        }
        
        log.info("=== 配置验证结束 ===");
    }
}
