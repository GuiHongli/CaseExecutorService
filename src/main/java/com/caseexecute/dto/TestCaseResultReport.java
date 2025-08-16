package com.caseexecute.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用例执行结果上报DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class TestCaseResultReport {
    
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
     * 执行状态 (SUCCESS/FAILED/TIMEOUT)
     */
    private String status;
    
    /**
     * 执行结果描述
     */
    private String result;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息（如果执行失败）
     */
    private String errorMessage;
    
    /**
     * 执行机IP
     */
    private String executorIp;
    
    /**
     * 用例集ID
     */
    private Long testCaseSetId;
}
