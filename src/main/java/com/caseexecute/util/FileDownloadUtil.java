package com.caseexecute.util;

import com.caseexecute.config.FileStorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件下载工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class FileDownloadUtil {
    
    private static FileStorageConfig fileStorageConfig;
    
    @Autowired
    public void setFileStorageConfig(FileStorageConfig config) {
        FileDownloadUtil.fileStorageConfig = config;
    }
    
    /**
     * 下载文件到/opt目录下的taskId子目录
     * 
     * @param url 文件URL
     * @param taskId 任务ID
     * @return 下载的文件路径
     * @throws Exception 下载异常
     */
    public static Path downloadFile(String url, String taskId) throws Exception {
        log.info("开始下载文件 - URL: {}, 任务ID: {}", url, taskId);
        
        // 获取配置的根目录
        String rootDir = fileStorageConfig != null ? fileStorageConfig.getRootDirectory() : "/opt";
        Path rootDirectory = Paths.get(rootDir);
        Path taskDir = rootDirectory.resolve(taskId);
        
        // 确保根目录存在
        if (!Files.exists(rootDirectory)) {
            log.warn("根目录 {} 不存在，尝试创建", rootDir);
            try {
                Files.createDirectories(rootDirectory);
            } catch (Exception e) {
                log.error("创建根目录失败: {}", e.getMessage());
                throw new RuntimeException("无法创建根目录 " + rootDir + ": " + e.getMessage());
            }
        }
        
        // 创建taskId子目录
        if (!Files.exists(taskDir)) {
            Files.createDirectories(taskDir);
            log.info("创建任务目录: {}", taskDir);
        }
        
        // 从URL中提取文件名
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        Path filePath = taskDir.resolve(fileName);
        
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
     * 下载文件（兼容旧版本，使用临时目录）
     * 
     * @param url 文件URL
     * @return 下载的文件路径
     * @throws Exception 下载异常
     */
    public static Path downloadFile(String url) throws Exception {
        log.info("开始下载文件到临时目录 - URL: {}", url);
        
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
     * 解压ZIP文件到/opt目录下的taskId子目录
     * 
     * @param zipFilePath ZIP文件路径
     * @param taskId 任务ID
     * @return 解压目录路径
     * @throws Exception 解压异常
     */
    public static Path extractZipFile(Path zipFilePath, String taskId) throws Exception {
        log.info("开始解压ZIP文件到/opt目录 - 路径: {}, 任务ID: {}", zipFilePath, taskId);
        
        // 获取配置的根目录
        String rootDir = fileStorageConfig != null ? fileStorageConfig.getRootDirectory() : "/opt";
        Path rootDirectory = Paths.get(rootDir);
        Path taskDir = rootDirectory.resolve(taskId);
        
        // 确保根目录存在
        if (!Files.exists(rootDirectory)) {
            log.warn("根目录 {} 不存在，尝试创建", rootDir);
            try {
                Files.createDirectories(rootDirectory);
            } catch (Exception e) {
                log.error("创建根目录失败: {}", e.getMessage());
                throw new RuntimeException("无法创建根目录 " + rootDir + ": " + e.getMessage());
            }
        }
        
        // 创建taskId子目录
        if (!Files.exists(taskDir)) {
            Files.createDirectories(taskDir);
            log.info("创建任务目录: {}", taskDir);
        }
        
        // 创建解压目录
        Path extractPath = taskDir.resolve("extracted");
        if (Files.exists(extractPath)) {
            // 如果目录已存在，先删除
            FileUtils.deleteDirectory(extractPath.toFile());
        }
        Files.createDirectories(extractPath);
        
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
     * 解压ZIP文件（兼容旧版本，使用临时目录）
     * 
     * @param zipFilePath ZIP文件路径
     * @return 解压目录路径
     * @throws Exception 解压异常
     */
    public static Path extractZipFile(Path zipFilePath) throws Exception {
        log.info("开始解压ZIP文件到临时目录 - 路径: {}", zipFilePath);
        
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
     * 清理文件或目录
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
                log.info("删除文件/目录: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("清理文件/目录失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清理任务目录
     * 
     * @param taskId 任务ID
     */
    public static void cleanupTaskDirectory(String taskId) {
        try {
            String rootDir = fileStorageConfig != null ? fileStorageConfig.getRootDirectory() : "/opt";
            Path taskDir = Paths.get(rootDir).resolve(taskId);
            if (Files.exists(taskDir)) {
                FileUtils.deleteDirectory(taskDir.toFile());
                log.info("清理任务目录: {}", taskDir);
            }
        } catch (Exception e) {
            log.warn("清理任务目录失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
    }
}
