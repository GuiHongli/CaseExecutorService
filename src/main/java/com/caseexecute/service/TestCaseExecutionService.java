package com.caseexecute.service;

import com.caseexecute.dto.TestCaseExecutionRequest;

/**
 * 用例执行服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface TestCaseExecutionService {
    
    /**
     * 处理用例执行任务
     * 
     * @param request 用例执行任务请求
     */
    void processTestCaseExecution(TestCaseExecutionRequest request);
    
    /**
     * 取消任务执行
     * 
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelTaskExecution(String taskId);
}
