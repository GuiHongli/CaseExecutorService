package com.caseexecute.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * GoHttpServer客户端工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
public class GoHttpServerClient {

    private final HttpClient httpClient;

    public GoHttpServerClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 上传本地文件到gohttpserver
     * @param localFilePath 本地文件路径
     * @param targetFileName 目标文件名
     * @param goHttpServerUrl gohttpserver地址
     * @return 上传后的文件URL
     */
    public String uploadLocalFile(String localFilePath, String targetFileName, String goHttpServerUrl) throws IOException {
        log.info("开始上传本地文件到gohttpserver: {} -> {}, 服务器地址: {}", localFilePath, targetFileName, goHttpServerUrl);
        
        try {
            Path sourcePath = Path.of(localFilePath);
            if (!Files.exists(sourcePath)) {
                throw new IOException("源文件不存在: " + localFilePath);
            }
            
            // 构建上传URL，使用gohttpserver的标准上传接口
            String uploadUrl = goHttpServerUrl + "/upload";
            
            // 读取文件内容
            byte[] fileBytes = Files.readAllBytes(sourcePath);
            
            // 构建multipart请求
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            byte[] multipartBody = buildMultipartBody(fileBytes, targetFileName, boundary);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String fileUrl = goHttpServerUrl + "/upload/" + targetFileName;
                log.info("本地文件上传成功: {}", fileUrl);
                return fileUrl;
            } else {
                throw new IOException("上传失败，HTTP状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
            
        } catch (Exception e) {
            log.error("上传本地文件到gohttpserver失败: {}", e.getMessage());
            throw new IOException("上传本地文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建multipart body
     */
    private byte[] buildMultipartBody(byte[] fileBytes, String fileName, String boundary) throws IOException {
        StringBuilder body = new StringBuilder();
        
        // 添加文件部分
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n");
        body.append("\r\n");
        
        // 转换为字节数组
        byte[] headerBytes = body.toString().getBytes("UTF-8");
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
        
        // 组合完整的multipart body
        byte[] multipartBody = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, multipartBody, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + fileBytes.length, footerBytes.length);
        
        return multipartBody;
    }
}
