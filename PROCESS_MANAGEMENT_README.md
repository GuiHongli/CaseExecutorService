# Python进程管理功能快速指南

## 功能概述
CaseExecuteService提供强大的Python进程管理功能，特别针对Windows系统优化，能够彻底清理Python脚本及其创建的所有子进程。

## 快速使用

### 1. 停止任务相关的所有进程
```bash
curl -X POST http://localhost:8081/api/test-case-execution/cancel/TASK_001
```

### 2. 紧急终止任务进程
```bash
curl -X POST http://localhost:8081/api/test-case-execution/emergency-stop/TASK_001
```

### 3. 按脚本路径终止进程
```bash
curl -X POST "http://localhost:8081/api/test-case-execution/emergency-stop-script?scriptPath=C:\scripts\test.py"
```

## 核心能力

✅ **Windows系统优化**：使用`taskkill`、`wmic`等Windows原生命令  
✅ **递归进程清理**：自动发现并终止所有子进程、孙进程  
✅ **多种终止方式**：支持按任务ID、脚本路径等方式终止  
✅ **强制终止**：确保所有相关进程被彻底清理  
✅ **完善日志**：详细记录所有操作过程  

## 支持的进程类型

- Python主进程
- subprocess创建的子进程  
- multiprocessing创建的进程
- 后台守护进程
- 任何Python脚本启动的其他进程

## 系统要求

- Windows 7/8/10/11 或 Windows Server
- 管理员权限（推荐）
- Java 8+

## 注意事项

1. 建议以管理员权限运行服务
2. 确保系统PATH包含Windows命令
3. 部分防病毒软件可能拦截进程操作
4. 重要数据请在停止前保存

详细文档请参考：[WINDOWS_PROCESS_MANAGEMENT.md](./WINDOWS_PROCESS_MANAGEMENT.md)
