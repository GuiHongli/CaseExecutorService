# CaseExecuteService API 文档

## 概述

CaseExecuteService 是用例执行服务，负责接收和处理用例执行任务。本文档描述了可用的API接口。

## 基础信息

- **服务名称**: CaseExecuteService
- **服务端口**: 8081 (默认)
- **基础路径**: `/test-case-execution`
- **数据格式**: JSON

## API 接口列表

### 1. 接收用例执行任务

#### 接口信息
- **URL**: `POST /test-case-execution/receive`
- **描述**: 接收用例执行任务，启动用例执行流程
- **Content-Type**: `application/json`

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| taskId | String | 是 | 用例执行任务ID，唯一标识符 |
| executorIp | String | 是 | 执行机IP地址 |
| testCaseSetId | Long | 是 | 用例集ID |
| testCaseSetPath | String | 是 | 用例集存储路径 |
| testCaseList | Array | 是 | 用例列表 |
| resultReportUrl | String | 是 | 结果上报URL |
| logReportUrl | String | 是 | 日志上报URL |

#### testCaseList 数组元素结构

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| testCaseId | Long | 是 | 用例ID |
| round | Integer | 是 | 执行轮次 |

#### 请求示例

```bash
curl -X POST "http://localhost:8081/test-case-execution/receive" \
  -H "Content-Type: application/json" \
  -d @test_case_execution_example.json
```

#### 请求数据示例

```json
{
  "taskId": "TASK_20240101_001",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1,
  "testCaseSetPath": "/uploads/testcase/网络测试用例集_v1.0.zip",
  "testCaseList": [
    {
      "testCaseId": 1,
      "round": 1
    },
    {
      "testCaseId": 2,
      "round": 1
    },
    {
      "testCaseId": 1,
      "round": 2
    },
    {
      "testCaseId": 2,
      "round": 2
    }
  ],
  "resultReportUrl": "http://192.168.1.50:8080/api/test-result/report",
  "logReportUrl": "http://192.168.1.50:8080/api/test-log/report"
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "用例执行任务接收成功",
  "data": {
    "taskId": "TASK_20240101_001",
    "status": "RECEIVED",
    "message": "用例执行任务已接收",
    "executorIp": "192.168.1.100",
    "testCaseCount": 4,
    "timestamp": 1704067200000
  },
  "timestamp": 1704067200000
}
```

#### 响应字段说明

| 字段名 | 类型 | 描述 |
|--------|------|------|
| code | Integer | 状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 响应数据 |
| data.taskId | String | 任务ID |
| data.status | String | 任务状态 |
| data.message | String | 任务消息 |
| data.executorIp | String | 执行机IP |
| data.testCaseCount | Integer | 用例数量 |
| data.timestamp | Long | 时间戳 |

### 2. 查询任务状态

#### 接口信息
- **URL**: `GET /test-case-execution/status/{taskId}`
- **描述**: 查询指定任务的执行状态
- **参数**: taskId (路径参数)

#### 请求示例

```bash
curl -X GET "http://localhost:8081/test-case-execution/status/TASK_20240101_001"
```

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "TASK_20240101_001",
    "status": "RUNNING",
    "progress": 50,
    "completedCases": 2,
    "totalCases": 4,
    "startTime": 1704067200000,
    "estimatedEndTime": 1704067260000
  },
  "timestamp": 1704067200000
}
```

#### 响应字段说明

| 字段名 | 类型 | 描述 |
|--------|------|------|
| data.status | String | 任务状态 (RECEIVED/RUNNING/COMPLETED/FAILED/CANCELLED) |
| data.progress | Integer | 执行进度 (0-100) |
| data.completedCases | Integer | 已完成的用例数量 |
| data.totalCases | Integer | 总用例数量 |
| data.startTime | Long | 开始时间戳 |
| data.estimatedEndTime | Long | 预计结束时间戳 |

### 3. 取消任务执行

#### 接口信息
- **URL**: `POST /test-case-execution/cancel/{taskId}`
- **描述**: 取消指定任务的执行
- **参数**: taskId (路径参数)

#### 请求示例

```bash
curl -X POST "http://localhost:8081/test-case-execution/cancel/TASK_20240101_001"
```

#### 响应示例

```json
{
  "code": 200,
  "message": "任务取消成功",
  "data": {
    "taskId": "TASK_20240101_001",
    "status": "CANCELLED",
    "message": "任务已取消",
    "timestamp": 1704067200000
  },
  "timestamp": 1704067200000
}
```

## 任务状态说明

| 状态 | 描述 |
|------|------|
| RECEIVED | 任务已接收，等待执行 |
| RUNNING | 任务正在执行中 |
| COMPLETED | 任务执行完成 |
| FAILED | 任务执行失败 |
| CANCELLED | 任务已取消 |

## 错误码说明

| 错误码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 任务不存在 |
| 500 | 服务器内部错误 |

## 使用流程

### 1. 发送执行任务
```bash
# 1. 准备请求数据
cat > request.json << EOF
{
  "taskId": "TASK_20240101_001",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1,
  "testCaseSetPath": "/uploads/testcase/网络测试用例集_v1.0.zip",
  "testCaseList": [
    {"testCaseId": 1, "round": 1},
    {"testCaseId": 2, "round": 1},
    {"testCaseId": 1, "round": 2}
  ],
  "resultReportUrl": "http://192.168.1.50:8080/api/test-result/report",
  "logReportUrl": "http://192.168.1.50:8080/api/test-log/report"
}
EOF

# 2. 发送请求
curl -X POST "http://localhost:8081/test-case-execution/receive" \
  -H "Content-Type: application/json" \
  -d @request.json
```

### 2. 监控任务状态
```bash
# 定期查询任务状态
while true; do
  curl -X GET "http://localhost:8081/test-case-execution/status/TASK_20240101_001"
  sleep 10
done
```

### 3. 取消任务（如需要）
```bash
curl -X POST "http://localhost:8081/test-case-execution/cancel/TASK_20240101_001"
```

## 注意事项

1. **任务ID唯一性**: 每个任务ID必须是唯一的，建议使用时间戳+随机数的方式生成
2. **文件路径**: 用例集路径必须是执行机可访问的路径
3. **网络连通性**: 确保执行机能够访问结果上报和日志上报的URL
4. **并发限制**: 同一执行机同时执行的任务数量有限制
5. **超时设置**: 建议设置合理的请求超时时间
6. **轮次说明**: 轮次用于标识同一用例的不同执行次数，通常用于稳定性测试

## 日志查看

服务运行时会输出详细的执行日志，可以通过以下方式查看：

```bash
# 查看服务日志
tail -f logs/case-execute-service.log

# 查看特定任务的日志
grep "TASK_20240101_001" logs/case-execute-service.log
```

## 测试数据

可以使用提供的示例文件进行测试：

```bash
curl -X POST "http://localhost:8081/test-case-execution/receive" \
  -H "Content-Type: application/json" \
  -d @test_case_execution_example.json
```
