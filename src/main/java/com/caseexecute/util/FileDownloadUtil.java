package com.caseexecute.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件下载工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
public class FileDownloadUtil {
    
    /**
     * 下载文件
     * 
     * @param url 文件URL
     * @return 下载的文件路径
     * @throws Exception 下载异常
     */
    public static Path downloadFile(String url) throws Exception {
        log.info("开始下载文件 - URL: {}", url);
        
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("download_");
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        Path filePath = tempDir.resolve(fileName);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("下载文件失败，HTTP状态码: " + response.getStatusLine().getStatusCode());
                }
                
                HttpEntity entity = response.getEntity();
                try (InputStream inputStream = entity.getContent();
                     FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
        }
        
        log.info("文件下载完成 - 路径: {}", filePath);
        return filePath;
    }
    
    /**
     * 解压ZIP文件
     * 
     * @param zipFilePath ZIP文件路径
     * @return 解压目录路径
     * @throws Exception 解压异常
     */
    public static Path extractZipFile(Path zipFilePath) throws Exception {
        log.info("开始解压ZIP文件 - 路径: {}", zipFilePath);
        
        // 创建解压目录
        Path extractPath = Files.createTempDirectory("extract_");
        
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = extractPath.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                        IOUtils.copy(zipInputStream, outputStream);
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        
        log.info("ZIP文件解压完成 - 路径: {}", extractPath);
        return extractPath;
    }
    
    /**
     * 清理临时文件
     * 
     * @param filePath 文件路径
     */
    public static void cleanupFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                if (Files.isDirectory(filePath)) {
                    FileUtils.deleteDirectory(filePath.toFile());
                } else {
                    Files.delete(filePath);
                }
                log.info("删除临时文件: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }
    }
}
