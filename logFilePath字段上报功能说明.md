# logFilePath字段上报功能说明

## 功能概述

在CaseExecuteService中增加了`logFilePath`字段的上报功能，确保用例执行结果上报时包含日志文件路径信息。

## 修改内容

### 1. TestCaseResultReport DTO类修改

**文件**: `CaseExecuteService/src/main/java/com/caseexecute/dto/TestCaseResultReport.java`

**修改内容**:
- 添加了`logFilePath`字段，用于存储日志文件路径或HTTP链接

```java
/**
 * 日志文件路径或HTTP链接
 */
private String logFilePath;
```

### 2. TestCaseExecutionServiceImpl类修改

**文件**: `CaseExecuteService/src/main/java/com/caseexecute/service/impl/TestCaseExecutionServiceImpl.java`

**修改内容**:

#### 2.1 新增带logFilePath参数的方法重载
```java
/**
 * 上报用例执行结果（带失败原因和日志文件路径）
 */
private void reportTestCaseResult(TestCaseExecutionRequest request,
                                TestCaseExecutionRequest.TestCaseInfo testCase,
                                String status,
                                String result,
                                Long executionTime,
                                java.time.LocalDateTime startTime,
                                java.time.LocalDateTime endTime,
                                String failureReason,
                                String logFilePath) {
    // 构建报告并设置logFilePath字段
    TestCaseResultReport report = new TestCaseResultReport();
    // ... 其他字段设置 ...
    report.setLogFilePath(logFilePath);
    
    // 上报结果
    httpReportUtil.reportTestCaseResult(request.getResultReportUrl(), report);
}
```

#### 2.2 修改原有方法调用
- 修改了所有`reportTestCaseResult`方法调用，添加`logFilePath`参数
- 正常执行成功时传递`executionResult.getLogFilePath()`
- 异常情况下传递`null`

**主要调用点修改**:
```java
// 正常执行成功后的上报
reportTestCaseResult(request, testCase, analysis.getStatus(), analysis.getResult(), 
        executionResult.getExecutionTime(), executionResult.getStartTime(), 
        executionResult.getEndTime(), analysis.getFailureReason(), 
        executionResult.getLogFilePath());

// 异常情况下的上报
reportTestCaseResult(request, testCase, "BLOCKED", "用例执行失败", 0L, null, null, failureReason, null);
```

## 数据流程

### 1. 用例执行阶段
1. `PythonExecutorUtil.executePythonScript()`执行Python脚本
2. 生成日志文件并保存到本地
3. 上传日志文件到gohttpserver（如果配置了地址）
4. 返回包含`logFilePath`的执行结果

### 2. 结果上报阶段
1. `TestCaseExecutionServiceImpl.reportTestCaseResult()`构建执行结果报告
2. 设置`TestCaseResultReport.logFilePath`字段
3. 通过`HttpReportUtil.reportTestCaseResult()`上报到DataCollectService

### 3. 数据接收阶段
1. DataCollectService的`TestCaseExecutionResultController.reportTestCaseResult()`接收结果
2. 结果中包含`logFilePath`字段
3. 保存到数据库并更新用例执行例次表

## 字段内容说明

### logFilePath字段可能包含的内容

#### 1. HTTP链接（上传到gohttpserver后）
```
http://localhost:8000/upload/TC001_1.log
```

#### 2. 本地文件路径
```
/Users/zhengtengsong/projects/ghl/cursor/DataCollectService/CaseExecuteService/logs/TC001_1.log
```

#### 3. 相对路径
```
logs/TC001_1.log
```

#### 4. null值
- 当用例执行失败或异常时
- 当没有配置gohttpserver地址时
- 当日志文件生成失败时

## 使用示例

### 1. 正常执行成功
```json
{
  "taskId": "TASK_20240101_001",
  "testCaseId": 1,
  "round": 1,
  "status": "SUCCESS",
  "result": "用例执行成功",
  "executionTime": 15000,
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-01T10:00:15",
  "failureReason": null,
  "logFilePath": "http://localhost:8000/upload/TC001_1.log",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1
}
```

### 2. 执行失败
```json
{
  "taskId": "TASK_20240101_001",
  "testCaseId": 1,
  "round": 1,
  "status": "FAILED",
  "result": "用例执行失败",
  "executionTime": 5000,
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-01T10:00:05",
  "failureReason": "网络连接超时",
  "logFilePath": "http://localhost:8000/upload/TC001_1.log",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1
}
```

### 3. 执行异常
```json
{
  "taskId": "TASK_20240101_001",
  "testCaseId": 1,
  "round": 1,
  "status": "BLOCKED",
  "result": "用例执行失败",
  "executionTime": 0,
  "startTime": null,
  "endTime": null,
  "failureReason": "Python脚本文件不存在",
  "logFilePath": null,
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1
}
```

## 注意事项

1. **向后兼容**: 原有的`reportTestCaseResult`方法仍然保留，通过重载方式调用新方法
2. **空值处理**: 异常情况下`logFilePath`为null，不会影响其他字段的上报
3. **日志记录**: 所有操作都有详细的日志记录，便于问题排查
4. **数据一致性**: 确保CaseExecuteService和DataCollectService的字段定义一致

## 相关文件

### 修改的文件
- `CaseExecuteService/src/main/java/com/caseexecute/dto/TestCaseResultReport.java`
- `CaseExecuteService/src/main/java/com/caseexecute/service/impl/TestCaseExecutionServiceImpl.java`

### 相关的文件
- `CaseExecuteService/src/main/java/com/caseexecute/util/PythonExecutorUtil.java`
- `CaseExecuteService/src/main/java/com/caseexecute/util/HttpReportUtil.java`
- `DataCollectService/src/main/java/com/datacollect/dto/TestCaseExecutionResult.java`
- `DataCollectService/src/main/java/com/datacollect/controller/TestCaseExecutionResultController.java`

## 测试建议

1. **功能测试**: 测试正常执行、执行失败、执行异常等各种情况下的logFilePath上报
2. **数据验证**: 验证DataCollectService正确接收和存储logFilePath字段
3. **前端显示**: 验证前端页面正确显示日志文件路径
4. **链接访问**: 验证HTTP链接可以正常访问日志文件
