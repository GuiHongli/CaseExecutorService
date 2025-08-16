package com.caseexecute.util;

import com.caseexecute.dto.TestCaseResultReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;

/**
 * HTTP上报工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
public class HttpReportUtil {
    
    /**
     * 上报用例执行结果
     * 
     * @param reportUrl 上报URL
     * @param report 执行结果
     * @return 是否上报成功
     */
    public static boolean reportTestCaseResult(String reportUrl, TestCaseResultReport report) {
        try {
            log.info("开始上报用例执行结果 - 用例ID: {}, 轮次: {}, 状态: {}", 
                    report.getTestCaseId(), report.getRound(), report.getStatus());
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(reportUrl);
                httpPost.setHeader("Content-Type", "application/json");
                
                String jsonBody = com.alibaba.fastjson.JSON.toJSONString(report);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        log.info("用例执行结果上报成功 - 用例ID: {}, 轮次: {}", 
                                report.getTestCaseId(), report.getRound());
                        return true;
                    } else {
                        log.error("用例执行结果上报失败 - 用例ID: {}, 轮次: {}, HTTP状态码: {}", 
                                report.getTestCaseId(), report.getRound(), response.getStatusLine().getStatusCode());
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
    public static boolean reportTestCaseLog(String logReportUrl, String logContent, String logFileName, 
                                          Long testCaseId, Integer round) {
        try {
            log.info("开始上报用例执行日志 - 用例ID: {}, 轮次: {}", testCaseId, round);
            
            // 构建日志上报URL
            String fullLogReportUrl = logReportUrl;
            if (!fullLogReportUrl.endsWith("/")) {
                fullLogReportUrl += "/";
            }
            fullLogReportUrl += logFileName;
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(fullLogReportUrl);
                httpPost.setHeader("Content-Type", "text/plain");
                httpPost.setEntity(new StringEntity(logContent, StandardCharsets.UTF_8));
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        log.info("用例执行日志上报成功 - 用例ID: {}, 轮次: {}", testCaseId, round);
                        return true;
                    } else {
                        log.error("用例执行日志上报失败 - 用例ID: {}, 轮次: {}, HTTP状态码: {}", 
                                testCaseId, round, response.getStatusLine().getStatusCode());
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
