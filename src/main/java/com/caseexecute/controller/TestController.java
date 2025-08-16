package com.caseexecute.controller;

import com.caseexecute.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "CaseExecuteService");
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("version", "1.0.0");
        
        log.info("健康检查请求");
        return Result.success("服务运行正常", data);
    }

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        log.info("测试接口调用");
        return Result.success("Hello, CaseExecuteService!");
    }
}
