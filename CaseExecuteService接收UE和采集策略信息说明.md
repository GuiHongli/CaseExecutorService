# CaseExecuteService接收UE和采集策略信息说明

## 概述

CaseExecuteService中的任务接收接口需要修改，以便能够接收和处理DataCollectService发送的执行机关联的UE全部信息和采集策略的所有信息，从而获得完整的上下文信息来执行测试用例。

## 修改内容

### 1. 扩展TestCaseExecutionRequest DTO

**文件**: `src/main/java/com/caseexecute/dto/TestCaseExecutionRequest.java`

**新增字段**:
- `ueList`: 执行机关联的UE全部信息列表
- `collectStrategyInfo`: 采集策略的所有信息

**新增内部类**:
- `UeInfo`: UE详细信息
- `CollectStrategyInfo`: 采集策略详细信息

**字段说明**:
```java
/**
 * 执行机关联的UE全部信息
 */
private List<UeInfo> ueList;

/**
 * 采集策略的所有信息
 */
private CollectStrategyInfo collectStrategyInfo;
```

### 2. 修改TestCaseExecutionController

**文件**: `src/main/java/com/caseexecute/controller/TestCaseExecutionController.java`

**修改receiveTestCaseExecution方法**:
1. 添加UE信息记录逻辑
2. 添加采集策略信息记录逻辑
3. 在返回结果中包含UE数量和采集策略信息状态

**新增日志记录**:
```java
// 记录UE信息详情
if (request.getUeList() != null && !request.getUeList().isEmpty()) {
    log.info("执行机关联的UE信息:");
    log.info("  - UE数量: {}", request.getUeList().size());
    for (TestCaseExecutionRequest.UeInfo ue : request.getUeList()) {
        log.info("  - UE ID: {}, 名称: {}, 用途: {}, 网络类型: {}, 品牌: {}, 状态: {}", 
                ue.getUeId(), ue.getName(), ue.getPurpose(), 
                ue.getNetworkTypeName(), ue.getBrand(), ue.getStatus());
    }
} else {
    log.warn("执行机未关联UE信息");
}

// 记录采集策略信息详情
if (request.getCollectStrategyInfo() != null) {
    TestCaseExecutionRequest.CollectStrategyInfo strategy = request.getCollectStrategyInfo();
    log.info("采集策略信息:");
    log.info("  - 策略ID: {}", strategy.getId());
    log.info("  - 策略名称: {}", strategy.getName());
    log.info("  - 采集次数: {}", strategy.getCollectCount());
    log.info("  - 业务大类: {}", strategy.getBusinessCategory());
    log.info("  - APP: {}", strategy.getApp());
    log.info("  - 意图: {}", strategy.getIntent());
    log.info("  - 自定义参数: {}", strategy.getCustomParams());
    log.info("  - 策略状态: {}", strategy.getStatus());
} else {
    log.warn("未提供采集策略信息");
}
```

**返回结果增强**:
```java
result.put("ueCount", request.getUeList() != null ? request.getUeList().size() : 0);
result.put("hasCollectStrategy", request.getCollectStrategyInfo() != null);
```

### 3. 修改TestCaseExecutionServiceImpl

**文件**: `src/main/java/com/caseexecute/service/impl/TestCaseExecutionServiceImpl.java`

**修改processTestCaseExecution方法**:
1. 在任务处理开始时调用`logTaskContextInfo()`方法
2. 记录完整的任务上下文信息

**新增logTaskContextInfo方法**:
```java
/**
 * 记录任务上下文信息（UE信息和采集策略信息）
 * 
 * @param request 用例执行任务请求
 */
private void logTaskContextInfo(TestCaseExecutionRequest request) {
    log.info("=== 任务上下文信息 ===");
    
    // 记录UE信息
    if (request.getUeList() != null && !request.getUeList().isEmpty()) {
        log.info("执行机关联的UE设备信息:");
        log.info("  - UE设备数量: {}", request.getUeList().size());
        for (TestCaseExecutionRequest.UeInfo ue : request.getUeList()) {
            log.info("  - UE ID: {}, 名称: {}, 用途: {}, 网络类型: {}, 品牌: {}, 端口: {}, 状态: {}", 
                    ue.getUeId(), ue.getName(), ue.getPurpose(), 
                    ue.getNetworkTypeName(), ue.getBrand(), ue.getPort(), ue.getStatus());
            if (ue.getDescription() != null && !ue.getDescription().trim().isEmpty()) {
                log.info("    - 描述: {}", ue.getDescription());
            }
        }
    } else {
        log.warn("执行机未关联UE设备信息");
    }
    
    // 记录采集策略信息
    if (request.getCollectStrategyInfo() != null) {
        TestCaseExecutionRequest.CollectStrategyInfo strategy = request.getCollectStrategyInfo();
        log.info("采集策略信息:");
        log.info("  - 策略ID: {}", strategy.getId());
        log.info("  - 策略名称: {}", strategy.getName());
        log.info("  - 采集次数: {}", strategy.getCollectCount());
        log.info("  - 业务大类: {}", strategy.getBusinessCategory());
        log.info("  - APP: {}", strategy.getApp());
        log.info("  - 意图: {}", strategy.getIntent());
        log.info("  - 策略状态: {}", strategy.getStatus());
        if (strategy.getCustomParams() != null && !strategy.getCustomParams().trim().isEmpty()) {
            log.info("  - 自定义参数: {}", strategy.getCustomParams());
        }
        if (strategy.getDescription() != null && !strategy.getDescription().trim().isEmpty()) {
            log.info("  - 策略描述: {}", strategy.getDescription());
        }
    } else {
        log.warn("未提供采集策略信息");
    }
    
    log.info("=== 任务上下文信息记录完成 ===");
}
```

## 接收到的数据结构

CaseExecuteService现在可以接收以下完整的数据结构：

```json
{
  "taskId": "TASK_1234567890_192_168_1_100",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1,
  "testCaseSetPath": "http://localhost:8000/upload/testcase.zip",
  "testCaseList": [
    {
      "testCaseId": 1,
      "testCaseNumber": "TC001",
      "round": 1
    }
  ],
  "ueList": [
    {
      "id": 1,
      "ueId": "UE001",
      "name": "UE-Android-01",
      "purpose": "短视频测试",
      "networkTypeId": 8,
      "networkTypeName": "4G",
      "brand": "Huawei",
      "port": "COM1",
      "description": "Android UE设备01",
      "status": 1
    }
  ],
  "collectStrategyInfo": {
    "id": 1,
    "name": "短视频采集策略",
    "collectCount": 3,
    "testCaseSetId": 1,
    "businessCategory": "短视频",
    "app": "抖音",
    "intent": "性能测试",
    "customParams": "{\"timeout\": 30}",
    "description": "短视频应用性能采集策略",
    "status": 1
  },
  "resultReportUrl": "http://localhost:8080/api/test-result/report",
  "logReportUrl": "http://localhost:8000"
}
```

## 日志记录示例

### 任务接收日志
```
接收到用例执行任务 - 任务ID: TASK_1234567890_192_168_1_100, 执行机IP: 192.168.1.100, 用例集ID: 1

任务详情:
  - 任务ID: TASK_1234567890_192_168_1_100
  - 执行机IP: 192.168.1.100
  - 用例集ID: 1
  - 用例集路径: http://localhost:8000/upload/testcase.zip
  - 用例数量: 5
  - 结果上报URL: http://localhost:8080/api/test-result/report
  - 日志上报URL: http://localhost:8000

执行机关联的UE信息:
  - UE数量: 3
  - UE ID: UE001, 名称: UE-Android-01, 用途: 短视频测试, 网络类型: 4G, 品牌: Huawei, 状态: 1
  - UE ID: UE002, 名称: UE-Android-02, 用途: 直播测试, 网络类型: 5G, 品牌: Huawei, 状态: 1
  - UE ID: UE003, 名称: UE-iOS-01, 用途: 游戏测试, 网络类型: 4G, 品牌: Apple, 状态: 1

采集策略信息:
  - 策略ID: 1
  - 策略名称: 短视频采集策略
  - 采集次数: 3
  - 业务大类: 短视频
  - APP: 抖音
  - 意图: 性能测试
  - 自定义参数: {"timeout": 30}
  - 策略状态: 1

用例列表:
  - 用例ID: 1, 用例编号: TC001, 轮次: 1
  - 用例ID: 2, 用例编号: TC002, 轮次: 1
  - 用例ID: 3, 用例编号: TC003, 轮次: 1
```

### 任务处理日志
```
开始处理用例执行任务 - 任务ID: TASK_1234567890_192_168_1_100

=== 任务上下文信息 ===
执行机关联的UE设备信息:
  - UE设备数量: 3
  - UE ID: UE001, 名称: UE-Android-01, 用途: 短视频测试, 网络类型: 4G, 品牌: Huawei, 端口: COM1, 状态: 1
    - 描述: Android UE设备01
  - UE ID: UE002, 名称: UE-Android-02, 用途: 直播测试, 网络类型: 5G, 品牌: Huawei, 端口: COM2, 状态: 1
    - 描述: Android UE设备02
  - UE ID: UE003, 名称: UE-iOS-01, 用途: 游戏测试, 网络类型: 4G, 品牌: Apple, 端口: COM3, 状态: 1
    - 描述: iOS UE设备01

采集策略信息:
  - 策略ID: 1
  - 策略名称: 短视频采集策略
  - 采集次数: 3
  - 业务大类: 短视频
  - APP: 抖音
  - 意图: 性能测试
  - 策略状态: 1
  - 自定义参数: {"timeout": 30}
  - 策略描述: 短视频应用性能采集策略
=== 任务上下文信息记录完成 ===
```

## 返回结果示例

```json
{
  "code": 200,
  "message": "用例执行任务接收成功",
  "data": {
    "taskId": "TASK_1234567890_192_168_1_100",
    "status": "RECEIVED",
    "message": "用例执行任务已接收",
    "executorIp": "192.168.1.100",
    "testCaseCount": 5,
    "ueCount": 3,
    "hasCollectStrategy": true,
    "timestamp": 1703123456789
  }
}
```

## 应用场景

### 1. UE设备管理
- 根据UE的网络类型选择合适的测试用例
- 根据UE的品牌和型号调整测试参数
- 监控UE设备状态，确保测试环境稳定

### 2. 采集策略执行
- 根据采集策略的业务大类和APP筛选测试用例
- 根据采集次数控制测试轮次
- 根据自定义参数调整测试行为
- 根据意图确定测试重点

### 3. 测试环境配置
- 根据UE的网络类型配置网络环境
- 根据UE的端口信息建立连接
- 根据采集策略的APP信息准备测试环境

### 4. 结果分析
- 结合UE信息和采集策略信息分析测试结果
- 根据业务大类和APP分类统计测试结果
- 根据UE设备类型分析性能差异

## 注意事项

1. **数据完整性**: 确保接收到的UE和采集策略信息完整
2. **错误处理**: 如果UE或采集策略信息为空，记录警告日志但不影响任务执行
3. **日志记录**: 详细记录所有上下文信息，便于问题排查和结果分析
4. **向后兼容**: 保持对旧版本请求的兼容性，UE和采集策略信息为可选字段

## 测试建议

1. **正常流程测试**: 发送包含完整UE和采集策略信息的请求
2. **异常流程测试**: 
   - 发送不包含UE信息的请求
   - 发送不包含采集策略信息的请求
   - 发送包含空UE列表的请求
3. **数据完整性测试**: 验证接收到的数据结构正确性
4. **日志验证测试**: 确认所有上下文信息都被正确记录
