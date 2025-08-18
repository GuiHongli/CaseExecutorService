#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
超时终止测试脚本
用于测试CaseExecuteService的超时终止功能
包含子进程创建，验证进程树终止
"""

import time
import sys
import logging
import subprocess
import os
import signal

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def create_child_process():
    """创建子进程"""
    try:
        # 创建一个长时间运行的子进程
        child_process = subprocess.Popen(
            ['python3', '-c', 'import time; print("子进程启动"); time.sleep(300); print("子进程完成")'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        logger.info(f"创建子进程 - PID: {child_process.pid}")
        return child_process
    except Exception as e:
        logger.error(f"创建子进程失败: {e}")
        return None

def test_timeout_termination():
    """测试超时终止功能"""
    logger.info("开始执行超时终止测试脚本")
    
    # 获取命令行参数
    if len(sys.argv) > 1:
        sleep_time = int(sys.argv[1])
    else:
        sleep_time = 60  # 默认睡眠1分钟
    
    logger.info(f"脚本将睡眠 {sleep_time} 秒")
    logger.info(f"当前进程PID: {os.getpid()}")
    
    # 创建子进程
    child_process = create_child_process()
    
    try:
        # 模拟长时间执行
        for i in range(sleep_time):
            if i % 5 == 0:  # 每5秒输出一次日志
                logger.info(f"执行进度: {i}/{sleep_time} 秒")
                
                # 检查子进程状态
                if child_process and child_process.poll() is None:
                    logger.info(f"子进程仍在运行 - PID: {child_process.pid}")
                else:
                    logger.warning("子进程已终止")
                    
            time.sleep(1)
        
        logger.info("脚本执行完成")
        print("PASS: 脚本正常执行完成")
        
    except KeyboardInterrupt:
        logger.warning("脚本被中断")
        print("FAIL: 脚本被中断")
        sys.exit(1)
    except Exception as e:
        logger.error(f"脚本执行异常: {e}")
        print(f"FAIL: 脚本执行异常 - {e}")
        sys.exit(1)
    finally:
        # 清理子进程
        if child_process and child_process.poll() is None:
            logger.info("清理子进程")
            child_process.terminate()
            try:
                child_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                child_process.kill()

if __name__ == "__main__":
    test_timeout_termination()
