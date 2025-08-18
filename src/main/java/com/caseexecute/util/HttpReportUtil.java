package com.caseexecute.util;

import com.caseexecute.dto.TestCaseResultReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * HTTP上报工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class HttpReportUtil {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 上报用例执行结果
     * 
     * @param reportUrl 上报URL
     * @param report 执行结果
     * @return 是否上报成功
     */
    public boolean reportTestCaseResult(String reportUrl, TestCaseResultReport report) {
        try {
            log.info("开始上报用例执行结果 - 用例ID: {}, 轮次: {}, 状态: {}", 
                    report.getTestCaseId(), report.getRound(), report.getStatus());
            log.info("上报URL: {}", reportUrl);
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(reportUrl);
                httpPost.setHeader("Content-Type", "application/json");
                
                String jsonBody = objectMapper.writeValueAsString(report);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
                
                // 记录请求体内容
                log.info("上报请求体内容 - 用例ID: {}, 轮次: {}", report.getTestCaseId(), report.getRound());
                log.info("请求体JSON: {}", jsonBody);
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = org.apache.http.util.EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    log.info("上报响应 - 用例ID: {}, 轮次: {}, HTTP状态码: {}, 响应体: {}", 
                            report.getTestCaseId(), report.getRound(), statusCode, responseBody);
                    
                    if (statusCode == 200) {
                        log.info("用例执行结果上报成功 - 用例ID: {}, 轮次: {}", 
                                report.getTestCaseId(), report.getRound());
                        return true;
                    } else {
                        log.error("用例执行结果上报失败 - 用例ID: {}, 轮次: {}, HTTP状态码: {}, 响应体: {}", 
                                report.getTestCaseId(), report.getRound(), statusCode, responseBody);
                        return false;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("上报用例执行结果失败 - 用例ID: {}, 轮次: {}, 错误: {}", 
                    report.getTestCaseId(), report.getRound(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 上报用例执行日志
     * 
     * @param logReportUrl 日志上报URL
     * @param logContent 日志内容
     * @param logFileName 日志文件名
     * @param testCaseId 用例ID
     * @param round 轮次
     * @return 是否上报成功
     */
    public boolean reportTestCaseLog(String logReportUrl, String logContent, String logFileName, 
                                          Long testCaseId, Integer round) {
        try {
            log.info("开始上报用例执行日志 - 用例ID: {}, 轮次: {}", testCaseId, round);
            
            // 构建日志上报URL
            String fullLogReportUrl = logReportUrl;
            if (!fullLogReportUrl.endsWith("/")) {
                fullLogReportUrl += "/";
            }
            fullLogReportUrl += logFileName;
            
            log.info("日志上报URL: {}", fullLogReportUrl);
            log.info("日志文件名: {}", logFileName);
            log.info("日志内容长度: {} 字符", logContent != null ? logContent.length() : 0);
            
            // 记录日志内容的前500个字符（避免日志过长）
            if (logContent != null && logContent.length() > 0) {
                String logPreview = logContent.length() > 500 ? 
                    logContent.substring(0, 500) + "..." : logContent;
                log.info("日志内容预览 - 用例ID: {}, 轮次: {}", testCaseId, round);
                log.info("日志预览: {}", logPreview);
            }
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(fullLogReportUrl);
                httpPost.setHeader("Content-Type", "text/plain");
                httpPost.setEntity(new StringEntity(logContent, StandardCharsets.UTF_8));
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = org.apache.http.util.EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    log.info("日志上报响应 - 用例ID: {}, 轮次: {}, HTTP状态码: {}, 响应体: {}", 
                            testCaseId, round, statusCode, responseBody);
                    
                    if (statusCode == 200) {
                        log.info("用例执行日志上报成功 - 用例ID: {}, 轮次: {}", testCaseId, round);
                        return true;
                    } else {
                        log.error("用例执行日志上报失败 - 用例ID: {}, 轮次: {}, HTTP状态码: {}, 响应体: {}", 
                                testCaseId, round, statusCode, responseBody);
                        return false;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("上报用例执行日志失败 - 用例ID: {}, 轮次: {}, 错误: {}", 
                    testCaseId, round, e.getMessage(), e);
            return false;
        }
    }
}
