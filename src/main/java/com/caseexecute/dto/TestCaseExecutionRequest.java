package com.caseexecute.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用例执行任务请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class TestCaseExecutionRequest {
    
    /**
     * 用例执行任务ID
     */
    @NotBlank(message = "用例执行任务ID不能为空")
    private String taskId;
    
    /**
     * 执行机IP
     */
    @NotBlank(message = "执行机IP不能为空")
    private String executorIp;
    
    /**
     * 用例集ID
     */
    @NotNull(message = "用例集ID不能为空")
    private Long testCaseSetId;
    
    /**
     * 用例集存储路径
     */
    @NotBlank(message = "用例集存储路径不能为空")
    private String testCaseSetPath;
    
    /**
     * 用例列表
     */
    @NotEmpty(message = "用例列表不能为空")
    private List<TestCaseInfo> testCaseList;
    
    /**
     * 结果上报URL
     */
    @NotBlank(message = "结果上报URL不能为空")
    private String resultReportUrl;
    
    /**
     * 日志上报URL
     */
    @NotBlank(message = "日志上报URL不能为空")
    private String logReportUrl;
    
    /**
     * 用例信息
     */
    @Data
    public static class TestCaseInfo {
        
        /**
         * 用例ID
         */
        @NotNull(message = "用例ID不能为空")
        private Long testCaseId;
        
        /**
         * 用例编号
         */
        private String testCaseNumber;
        
        /**
         * 轮次
         */
        @NotNull(message = "轮次不能为空")
        private Integer round;
    }
}
