package com.caseexecute;

import com.caseexecute.config.FileStorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 用例执行服务启动类
 * 
 * @author system
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableConfigurationProperties({FileStorageConfig.class})
public class CaseExecuteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaseExecuteServiceApplication.class, args);
        System.out.println("用例执行服务启动成功！");
    }
}
