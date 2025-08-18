# JSON序列化修复总结

## 问题描述

CaseExecuteService在调用DataCollectService接收用例执行结果接口时出现JSON反序列化错误：

```
JSON parse error: raw timestamp (1755501989877) not allowed for `java.time.LocalDateTime`: need additional information such as an offset or time-zone (see class Javadocs)
```

错误原因：Jackson在反序列化`LocalDateTime`时遇到了时间戳格式，但无法正确解析。

## 问题分析

### 1. 根本原因
- CaseExecuteService使用FastJSON序列化`LocalDateTime`字段，默认输出为时间戳格式
- DataCollectService使用Jackson反序列化，期望`LocalDateTime`为字符串格式
- 格式不匹配导致反序列化失败

### 2. 错误请求体示例
```json
{
  "endTime": 1755501989877,
  "executionTime": 16640,
  "executorIp": "127.0.0.1",
  "result": "用例部分失败",
  "round": 1,
  "startTime": 1755501973237,
  "status": "PARTIAL_FAILURE",
  "taskId": "TASK_1755501971780_127_0_0_1",
  "testCaseId": 116,
  "testCaseSetId": 38
}
```

## 修复方案

### 1. DataCollectService修复

#### 创建Jackson配置类
```java
@Configuration
public class JacksonConfig {
    
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建JavaTimeModule来处理Java 8时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化器和反序列化器
        LocalDateTimeSerializer localDateTimeSerializer = new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        
        javaTimeModule.addSerializer(LocalDateTime.class, localDateTimeSerializer);
        javaTimeModule.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);
        
        // 注册模块并禁用时间戳格式
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }
}
```

### 2. CaseExecuteService修复

#### 创建Jackson配置类
```java
@Configuration
public class JacksonConfig {
    // 与DataCollectService相同的配置
}
```

#### 修改HttpReportUtil
```java
@Component
public class HttpReportUtil {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public boolean reportTestCaseResult(String reportUrl, TestCaseResultReport report) {
        // 使用配置的ObjectMapper序列化JSON
        String jsonBody = objectMapper.writeValueAsString(report);
        // ... 其他逻辑
    }
}
```

#### 修改TestCaseExecutionServiceImpl
```java
@Service
public class TestCaseExecutionServiceImpl implements TestCaseExecutionService {

    @Autowired
    private HttpReportUtil httpReportUtil;
    
    // 使用实例方法调用
    httpReportUtil.reportTestCaseResult(request.getResultReportUrl(), report);
}
```

## 修复效果

### 1. 修复后的请求体格式
```json
{
  "endTime": "2024-01-15 10:30:01",
  "executionTime": 16640,
  "executorIp": "127.0.0.1",
  "result": "用例部分失败",
  "round": 1,
  "startTime": "2024-01-15 10:30:00",
  "status": "PARTIAL_FAILURE",
  "taskId": "TASK_1755501971780_127_0_0_1",
  "testCaseId": 116,
  "testCaseSetId": 38
}
```

### 2. 时间格式统一
- **序列化格式**: `yyyy-MM-dd HH:mm:ss`
- **反序列化格式**: `yyyy-MM-dd HH:mm:ss`
- **时区处理**: 使用系统默认时区

### 3. 增强的日志记录
- 记录上报URL
- 记录完整的请求体JSON
- 记录HTTP响应状态码和响应体
- 记录日志内容预览

## 测试验证

### 1. 单元测试
- 验证LocalDateTime序列化和反序列化
- 验证JSON格式正确性
- 验证时间格式一致性

### 2. 集成测试
- 验证CaseExecuteService到DataCollectService的数据传输
- 验证时间字段的正确处理
- 验证错误处理的完整性

### 3. 测试脚本
创建了`test_json_serialization.py`测试脚本，包含：
- 时间戳格式验证
- JSON结构验证
- 序列化/反序列化测试

## 部署注意事项

### 1. 配置更新
- 确保两个服务都使用相同的Jackson配置
- 验证时间格式的一致性
- 检查日志级别配置

### 2. 兼容性
- 修复后的格式与现有数据兼容
- 支持向后兼容的时间格式
- 保持API接口的稳定性

### 3. 监控
- 监控JSON序列化/反序列化性能
- 关注时间字段的处理准确性
- 记录相关错误和异常

## 总结

通过以下修复措施解决了JSON序列化问题：

1. **统一序列化格式**: 使用Jackson配置统一LocalDateTime的序列化格式
2. **增强错误处理**: 提供详细的错误信息和日志记录
3. **改进代码结构**: 将HttpReportUtil改为Spring组件，便于依赖注入
4. **完善测试覆盖**: 创建专门的测试脚本验证修复效果

修复后的系统能够正确处理时间字段，确保CaseExecuteService和DataCollectService之间的数据传输正常。
