#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
超时测试脚本
用于测试用例执行服务的超时功能
"""

import time
import sys
import logging

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def test_timeout():
    """测试超时功能"""
    logger.info("开始执行超时测试脚本")
    
    # 获取命令行参数
    if len(sys.argv) > 1:
        sleep_time = int(sys.argv[1])
    else:
        sleep_time = 120  # 默认睡眠2分钟
    
    logger.info(f"脚本将睡眠 {sleep_time} 秒")
    
    try:
        # 模拟长时间执行
        for i in range(sleep_time):
            if i % 10 == 0:  # 每10秒输出一次日志
                logger.info(f"执行进度: {i}/{sleep_time} 秒")
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

if __name__ == "__main__":
    test_timeout()
