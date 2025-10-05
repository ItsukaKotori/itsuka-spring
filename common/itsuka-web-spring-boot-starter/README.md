# itsuka-web-spring-boot-starter

基于Spring Boot的Web增强Starter，提供统一的异常处理、请求响应日志、性能监控等Web开发常用功能。

## 🎯 核心功能

### 全局异常处理
- 统一处理业务异常、验证异常、HTTP异常
- 支持Spring Security异常处理
- 自动返回标准化响应格式
- 支持多语言错误消息

### 📊 请求响应监控
- 自动记录请求响应日志
- 性能监控和统计
- 支持自定义拦截器

### ⚙️ Web配置增强
- Jackson序列化配置优化
- RestTemplate自动配置
- WebMvcConfigurer增强

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.itsuka:itsuka-web-spring-boot-starter:1.0.0'
}
```

### 2. 自动配置

Starter会自动配置以下组件：
- `GlobalExceptionHandler` - 全局异常处理器
- `ObjectMapper` - Jackson配置
- `RestTemplate` - HTTP客户端
- 请求响应拦截器

### 3. 使用示例

#### 业务异常处理

```java
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public Response<User> getUser(@PathVariable Long id) {
        // 抛出业务异常，会被全局异常处理器捕获
        if (id <= 0) {
            throw new BusinessException("用户ID无效");
        }
        
        User user = userService.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return Response.success(user);
    }
}
```

#### 参数验证异常处理

```java
@RestController
@Validated
public class UserController {
    
    @PostMapping("/users")
    public Response<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // 验证失败会自动返回标准错误响应
        return Response.success(userService.create(request));
    }
}

@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

## 🔍 异常处理详解

### 支持的异常类型

| 异常类型 | 处理方式 | HTTP状态码 |
|---------|---------|----------|
| `BusinessException` | 返回业务错误信息 | 400 |
| `MethodArgumentNotValidException` | 返回验证错误详情 | 400 |
| `ConstraintViolationException` | 返回约束验证错误 | 400 |
| `HttpRequestMethodNotSupportedException` | 返回方法不支持错误 | 405 |
| `HttpMediaTypeNotSupportedException` | 返回媒体类型不支持错误 | 415 |
| `AccessDeniedException` | 返回访问拒绝错误 | 403 |
| `AuthenticationException` | 返回认证失败错误 | 401 |
| `Exception` | 返回通用服务器错误 | 500 |

### 响应格式

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null,
  "timestamp": "2023-12-07T10:30:00",
  "path": "/api/users"
}
```

## ⚙️ 配置说明

### Jackson配置

```yaml
spring:
  jackson:
    # 以下配置由starter自动应用
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

### 日志配置

```yaml
logging:
  level:
    org.itsuka.web.interceptor: DEBUG  # 开启请求日志
```

## 🔧 自定义配置

### 自定义异常处理

```java
@Component
public class CustomExceptionHandler {
    
    @EventListener
    public void handleCustomException(CustomException ex) {
        // 自定义异常处理逻辑
        log.error("Custom exception occurred", ex);
    }
}
```

### 自定义拦截器

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CustomInterceptor())
                .addPathPatterns("/api/**");
    }
}
```

## 📚 依赖模块

- `itsuka-core` - 核心工具类和响应模型
- `spring-boot-starter-web` - Spring Boot Web支持
- `spring-boot-starter-validation` - 参数验证支持

## ⚠️ 注意事项

1. **异常处理优先级**：自定义异常处理器会覆盖全局异常处理器的相同异常类型处理
2. **日志级别**：生产环境建议将拦截器日志级别设置为INFO或以上
3. **性能考虑**：请求响应日志会增加一定的性能开销，可根据需要开启或关闭
4. **安全考虑**：异常信息中避免暴露敏感信息，如数据库连接信息等