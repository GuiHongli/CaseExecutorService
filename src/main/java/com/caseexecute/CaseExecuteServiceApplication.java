package com.caseexecute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用例执行服务启动类
 * 
 * @author system
 * @since 2024-01-01
 */
@SpringBootApplication
public class CaseExecuteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaseExecuteServiceApplication.class, args);
        System.out.println("用例执行服务启动成功！");
    }
}
