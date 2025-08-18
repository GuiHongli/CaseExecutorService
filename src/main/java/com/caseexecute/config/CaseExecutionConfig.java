package com.caseexecute.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用例执行配置类
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "case.execution")
public class CaseExecutionConfig {
    
    /**
     * 单个用例执行超时时长（分钟），默认1分钟
     */
    private Integer timeoutMinutes = 1;
}
