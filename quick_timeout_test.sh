#!/bin/bash

# 快速超时终止测试脚本

echo "=== 超时终止功能快速测试 ==="

# 设置测试参数
TIMEOUT_MINUTES=0.1  # 6秒超时
SCRIPT_SLEEP_TIME=30  # 脚本睡眠30秒

echo "配置超时时间: ${TIMEOUT_MINUTES} 分钟 (${TIMEOUT_MINUTES} * 60 = $(echo "$TIMEOUT_MINUTES * 60" | bc) 秒)"
echo "脚本执行时间: ${SCRIPT_SLEEP_TIME} 秒"
echo "预期结果: 脚本应该在 $(echo "$TIMEOUT_MINUTES * 60" | bc) 秒后被终止"

# 检查Python进程
echo ""
echo "=== 测试前检查Python进程 ==="
ps aux | grep python | grep -v grep || echo "没有找到Python进程"

# 启动测试脚本
echo ""
echo "=== 启动测试脚本 ==="
python3 test_timeout_script.py ${SCRIPT_SLEEP_TIME} &
SCRIPT_PID=$!
echo "测试脚本PID: ${SCRIPT_PID}"

# 等待超时时间
echo ""
echo "=== 等待超时时间 ==="
sleep $(echo "$TIMEOUT_MINUTES * 60" | bc)

# 检查进程状态
echo ""
echo "=== 检查进程状态 ==="
if ps -p ${SCRIPT_PID} > /dev/null 2>&1; then
    echo "❌ 进程仍在运行 (PID: ${SCRIPT_PID})"
    echo "强制终止进程..."
    kill -9 ${SCRIPT_PID} 2>/dev/null
else
    echo "✅ 进程已被终止 (PID: ${SCRIPT_PID})"
fi

# 最终检查
echo ""
echo "=== 最终检查Python进程 ==="
ps aux | grep python | grep -v grep || echo "没有找到Python进程"

echo ""
echo "=== 测试完成 ==="
