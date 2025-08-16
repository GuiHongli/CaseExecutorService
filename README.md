# CaseExecuteService 用例执行服务

## 功能概述

CaseExecuteService 是用例执行服务，负责接收和处理用例执行任务。主要功能包括：

### 1. 用例执行任务接收
- 提供 `POST /test-case-execution/receive` 接口接收用例执行任务
- 支持异步处理，避免阻塞接口响应
- 包含任务ID、执行机IP、用例集路径、用例列表、上报URL等参数

### 2. 用例集文件处理
- 从gohttpserver下载用例集ZIP文件
- 自动解压到临时目录
- 执行完成后自动清理临时文件

### 3. Python脚本执行
- 在解压目录的 `cases` 文件夹中查找对应的Python脚本（格式：用例ID.py）
- 使用 `python` 命令执行脚本
- 记录执行时间、控制台输出、退出码
- 设置5分钟超时，超时后强制终止

### 4. 执行结果判断
- 根据Python脚本的退出码判断（非0表示失败）
- 根据控制台输出内容判断（包含"FAIL"/"ERROR"/"失败"表示失败）
- 支持超时检测（超过5分钟自动判定为超时）

### 5. 结果上报
- 执行结果上报到DataCollectService的POST接口
- 执行日志上报到gohttpserver的目录路径
- 异步上报，不阻塞主流程

### 6. 任务管理
- 提供任务状态查询接口 `GET /test-case-execution/status/{taskId}`
- 提供任务取消接口 `POST /test-case-execution/cancel/{taskId}`

## 技术栈

- **框架**: Spring Boot 2.7.18
- **HTTP客户端**: Apache HttpClient
- **文件处理**: Apache Commons IO
- **JSON处理**: FastJSON
- **日志**: SLF4J + Lombok

## 快速开始

### 1. 启动服务
```bash
mvn spring-boot:run
```

### 2. 发送执行任务
```bash
curl -X POST "http://localhost:8081/test-case-execution/receive" \
  -H "Content-Type: application/json" \
  -d @test_case_execution_example.json
```

### 3. 查询任务状态
```bash
curl -X GET "http://localhost:8081/test-case-execution/status/TASK_20240101_001"
```

## 配置要求

- **Java**: JDK 1.8+
- **Python**: Python 3.x（需要可执行 `python` 命令）
- **网络**: 能够访问gohttpserver和DataCollectService
