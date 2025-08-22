# CaseExecuteService Windows系统Python进程管理功能

## 概述

CaseExecuteService在Windows系统上提供了完整的Python进程管理能力，能够有效终止Python脚本运行中创建的所有相关进程，包括主进程、子进程、孙进程等，确保任务停止时不会有进程残留。

## 核心特性

### 🎯 问题解决
- ✅ **完全进程清理**：终止Python脚本及其创建的所有子进程
- ✅ **递归进程查找**：自动发现并终止进程树中的所有层级
- ✅ **跨平台支持**：针对Windows系统优化的进程管理机制
- ✅ **多种终止方式**：支持按任务ID、脚本路径等多种方式终止进程

### 🔧 技术架构

#### 1. 三层进程查找机制
```java
// 第一步：查找直接包含任务ID/脚本名称的Python进程
Set<Long> pythonPids = new HashSet<>();
Set<Long> allRelatedPids = new HashSet<>();

// 第二步：递归查找这些Python进程的所有子进程
for (Long pythonPid : pythonPids) {
    findAndAddChildProcesses(pythonPid, allRelatedPids);
}

// 第三步：终止所有相关进程
for (Long pid : allRelatedPids) {
    terminateProcessTreeByPidWindows(pid);
}
```

#### 2. 递归子进程发现
- 使用`wmic`命令查找父子进程关系
- 递归遍历整个进程树
- 避免重复添加和无限循环

#### 3. 强制进程终止
- 使用`taskkill /F /T /PID`命令
- 强制终止整个进程树
- 完善的错误处理和日志记录

## API接口

### 1. 任务取消接口
```http
POST /api/test-case-execution/cancel/{taskId}
```
**功能**：取消指定任务的所有Python进程
**参数**：
- `taskId`: 任务ID

**示例**：
```bash
curl -X POST http://localhost:8081/api/test-case-execution/cancel/TASK_001
```

### 2. 紧急终止接口（按任务ID）
```http
POST /api/test-case-execution/emergency-stop/{taskId}
```
**功能**：紧急终止指定任务ID的所有Python进程
**参数**：
- `taskId`: 任务ID

**示例**：
```bash
curl -X POST http://localhost:8081/api/test-case-execution/emergency-stop/TASK_001
```

### 3. 紧急终止接口（按脚本路径）
```http
POST /api/test-case-execution/emergency-stop-script?scriptPath={scriptPath}
```
**功能**：紧急终止指定脚本路径的所有Python进程
**参数**：
- `scriptPath`: 脚本完整路径

**示例**：
```bash
curl -X POST "http://localhost:8081/api/test-case-execution/emergency-stop-script?scriptPath=C:\scripts\test_network.py"
```

## Windows系统命令详解

### 1. 进程列表获取
```cmd
tasklist /FO CSV /V
```
- **功能**：获取系统中所有进程的详细信息
- **格式**：CSV格式输出，便于解析
- **用途**：查找包含特定任务ID或脚本名称的Python进程

### 2. 子进程查找
```cmd
wmic process where ParentProcessId=<pid> get ProcessId /format:csv
```
- **功能**：查找指定父进程的所有直接子进程
- **参数**：`<pid>` 父进程ID
- **用途**：递归构建完整的进程树

### 3. 进程树终止
```cmd
taskkill /F /T /PID <pid>
```
- **参数说明**：
  - `/F`：强制终止进程
  - `/T`：终止指定进程及其子进程
  - `/PID`：指定进程ID
- **用途**：彻底清理进程树

## 支持的进程类型

### ✅ 能够终止的进程
- **Python主进程**：直接执行的Python脚本进程
- **子进程**：通过`subprocess`、`multiprocessing`等模块创建的进程
- **孙进程**：子进程再次创建的进程（递归查找）
- **后台进程**：Python脚本启动的后台服务
- **守护进程**：长期运行的守护进程
- **其他进程**：任何由Python脚本直接或间接启动的进程

### 🔍 进程识别机制
1. **按任务ID识别**：查找命令行包含指定任务ID的Python进程
2. **按脚本路径识别**：查找命令行包含指定脚本名称的Python进程
3. **父子关系识别**：通过进程父子关系递归查找相关进程

## 使用场景

### 1. 正常任务停止
当用户主动停止测试任务时，系统会：
1. 调用`cancelTaskExecution`方法
2. 先尝试优雅停止Java层面的任务
3. 然后调用`terminateAllPythonProcessesByTaskId`彻底清理Python进程

### 2. 紧急进程清理
当任务异常或需要强制停止时：
1. 直接调用紧急终止接口
2. 跳过优雅停止流程
3. 立即强制终止所有相关进程

### 3. 脚本级别清理
当需要停止特定脚本的所有实例时：
1. 使用脚本路径终止接口
2. 查找所有运行该脚本的进程
3. 递归终止其创建的所有子进程

## 安全特性

### 🛡️ 错误处理
- **独立异常处理**：每个进程终止操作都有独立的异常处理
- **失败隔离**：单个进程终止失败不影响其他进程
- **降级机制**：Windows命令失败时自动降级到Java API
- **详细日志**：记录所有操作的详细日志，便于问题排查

### 🔒 进程验证
- **PID验证**：验证进程ID的有效性
- **自身保护**：避免终止服务自身进程
- **循环检测**：防止无限递归查找子进程
- **权限检查**：确保有足够权限执行系统命令

### ⏱️ 超时控制
- **命令超时**：每个系统命令都有超时限制（5-10秒）
- **防止卡死**：避免进程查找或终止操作无限等待
- **资源保护**：及时释放系统资源

## 日志示例

### 正常流程日志
```
2025-08-22 11:30:00 [INFO] 开始查找任务ID为 TASK_001 的所有相关进程
2025-08-22 11:30:01 [INFO] 找到Python主进程，PID: 1234, 进程名: python.exe
2025-08-22 11:30:01 [INFO] 找到子进程，父进程PID: 1234, 子进程PID: 5678
2025-08-22 11:30:01 [INFO] 找到子进程，父进程PID: 5678, 子进程PID: 9012
2025-08-22 11:30:02 [INFO] 终止进程，PID: 1234
2025-08-22 11:30:02 [INFO] 已终止进程树（Windows），PID: 1234
2025-08-22 11:30:02 [INFO] 终止进程，PID: 5678
2025-08-22 11:30:03 [INFO] 已终止进程树（Windows），PID: 5678
2025-08-22 11:30:03 [INFO] 终止进程，PID: 9012
2025-08-22 11:30:03 [INFO] 已终止进程树（Windows），PID: 9012
2025-08-22 11:30:03 [INFO] 已终止任务ID为 TASK_001 的所有相关进程，共 3 个进程
```

### 异常处理日志
```
2025-08-22 11:30:00 [WARN] 无法解析进程ID: abc123
2025-08-22 11:30:01 [WARN] 终止进程失败，PID: 5678 - 错误: Process not found
2025-08-22 11:30:02 [ERROR] 在Windows系统上终止Python进程失败: Command execution timeout
```

## 配置要求

### 系统要求
- **操作系统**：Windows 7/8/10/11 或 Windows Server 2008/2012/2016/2019/2022
- **权限**：管理员权限（用于执行taskkill和wmic命令）
- **Java版本**：Java 8+ （当前配置为Java 17，兼容Java 21）

### 环境变量
- **PATH**：确保系统PATH包含Windows系统命令目录
- **JAVA_HOME**：正确配置Java环境

### 端口配置
- **默认端口**：8081
- **配置文件**：`src/main/resources/application.yml`

## 性能特点

### 🚀 执行效率
- **并发处理**：使用线程池并发处理多个进程终止操作
- **快速查找**：优化的进程查找算法，通常在1-3秒内完成
- **批量操作**：一次操作可终止整个进程树

### 💾 资源占用
- **内存使用**：轻量级实现，内存占用minimal
- **CPU负载**：短时间CPU占用，操作完成后立即释放
- **网络开销**：仅涉及本地系统调用，无网络开销

## 最佳实践

### 📋 使用建议
1. **优先使用正常停止**：优先使用`/cancel`接口进行优雅停止
2. **紧急情况使用强制停止**：仅在必要时使用`/emergency-stop`接口
3. **监控日志**：定期检查日志确保进程正确清理
4. **权限管理**：确保服务以适当权限运行

### ⚠️ 注意事项
1. **管理员权限**：某些Windows系统命令需要管理员权限
2. **防病毒软件**：部分防病毒软件可能拦截进程终止操作
3. **系统负载**：大量进程终止操作可能短时间增加系统负载
4. **数据保存**：确保重要数据在进程终止前已保存

## 故障排除

### 常见问题
1. **权限不足**：确保以管理员身份运行服务
2. **命令不存在**：检查系统PATH配置
3. **进程无法终止**：某些系统进程受保护，无法强制终止
4. **超时问题**：网络或系统负载过高可能导致命令超时

### 解决方案
1. **检查权限**：使用`whoami`命令确认当前用户权限
2. **验证命令**：手动执行`tasklist`和`wmic`命令确认可用性
3. **查看日志**：详细查看服务日志定位具体问题
4. **重启服务**：必要时重启CaseExecuteService服务

---

*最后更新时间：2025-08-22*
*版本：v1.0.0*
