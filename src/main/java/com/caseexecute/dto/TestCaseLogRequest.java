package com.caseexecute.dto;

import lombok.Data;

/**
 * 用例执行日志请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class TestCaseLogRequest {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用例ID
     */
    private Long testCaseId;
    
    /**
     * 轮次
     */
    private Integer round;
    
    /**
     * 日志文件名
     */
    private String logFileName;
    
    /**
     * 日志内容
     */
    private String logContent;
    
    /**
     * 执行机IP
     */
    private String executorIp;
    
    /**
     * 用例集ID
     */
    private Long testCaseSetId;
}
