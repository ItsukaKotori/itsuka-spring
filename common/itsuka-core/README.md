# Itsuka Core

核心工具与通用模型模块，为整个项目提供基础能力支撑。包含统一的响应模型、异常体系、工具类库等核心组件。

## 🚀 功能概览

### 📊 通用响应模型
- **Response<T>** - 统一API响应格式，支持成功/失败状态、错误码、时间戳、请求ID等
- **Result<T>** - 业务逻辑结果包装类，支持函数式编程风格（map、flatMap、filter等）
- **Result与Response互转** - 提供toResponse()方法便于内部Result转换为API响应

### 👤 用户认证模型
- **LoginUser** - 实现Spring Security的UserDetails接口，包含用户基本信息和权限
- **AnonymousLoginUser** - 匿名用户实现，用于处理未登录状态

### ⚠️ 异常体系
- **ServiceException** - 业务异常基类，支持错误码和消息定制
- **AuthenticationException** - 认证异常，继承自ServiceException
- **统一异常处理** - 为上层业务提供一致的异常处理机制

### 🛠️ 工具类库
- **Assert** - 断言工具类，失败时抛出ServiceException
- **ReflectionKit** - 反射工具类，提供字段访问、方法调用、类型判断等功能
- **支持缓存** - 反射操作使用缓存机制提升性能

### 📁 内容类型
- **ContentType** - 文件MIME类型枚举，支持常见文件格式识别
- **扩展名映射** - 根据文件扩展名或文件名智能识别内容类型
- **性能优化** - 使用缓存映射提高查找效率

### 📋 常量定义
- **Constants** - 项目通用常量（响应码、日期格式器等）
- **标准化** - 统一的成功/失败响应码定义

## 📦 安装步骤

在需要使用此模块的 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-core')
}
```

## 💻 使用示例

### Response 统一响应格式

```java
import org.itsuka.core.domain.Response;

@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public Response<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user != null) {
            return Response.success(user);
        } else {
            return Response.error("User not found");
        }
    }
    
    @PostMapping("/users")
    public Response<User> createUser(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.create(request);
            return Response.success("用户创建成功", user);
        } catch (ServiceException e) {
            return Response.error(e);
        }
    }
}
```

### Result 业务逻辑处理

```java
import org.itsuka.core.domain.Result;

@Service
public class UserService {
    
    public Result<User> processUser(Long userId) {
        return Result.of(() -> userRepository.findById(userId))
                .filter(user -> user.isActive(), "用户已禁用")
                .map(this::enrichUserData)
                .onSuccess(user -> log.info("用户处理成功: {}", user.getId()))
                .onFailure(error -> log.error("用户处理失败: {}", error));
    }
    
    public Response<User> getUserResponse(Long userId) {
        return processUser(userId).toResponse();
    }
}
```

### Assert 断言工具

```java
import org.itsuka.core.utils.Assert;

@Service
public class OrderService {
    
    public void createOrder(CreateOrderRequest request) {
        // 参数校验
        Assert.notNull(request, "请求参数不能为空");
        Assert.hasText(request.getProductId(), "商品ID不能为空");
        Assert.positive(request.getQuantity(), "商品数量必须大于0");
        
        // 业务校验
        Product product = productService.findById(request.getProductId());
        Assert.notNull(product, 404, "商品不存在");
        Assert.state(product.getStock() >= request.getQuantity(), "库存不足");
        
        // 创建订单逻辑...
    }
}
```

### ReflectionKit 反射工具

```java
import org.itsuka.core.utils.ReflectionKit;

@Component
public class BeanUtils {
    
    public <T> T copyProperties(Object source, Class<T> targetClass) {
        T target = ReflectionKit.newInstance(targetClass);
        
        // 获取源对象所有字段
        Map<String, Object> sourceFields = ReflectionKit.toMap(source, true);
        
        // 设置目标对象字段
        sourceFields.forEach((fieldName, value) -> {
            try {
                ReflectionKit.setFieldValue(target, fieldName, value);
            } catch (Exception e) {
                // 忽略无法设置的字段
            }
        });
        
        return target;
    }
    
    public boolean hasField(Class<?> clazz, String fieldName) {
        try {
            ReflectionKit.getField(clazz, fieldName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### ContentType 文件类型识别

```java
import org.itsuka.core.enums.ContentType;

@Service
public class FileService {
    
    public String detectContentType(String filename) {
        ContentType contentType = ContentType.fromFilename(filename);
        return contentType.getMimeType();
    }
    
    public boolean isImageFile(String filename) {
        ContentType contentType = ContentType.fromFilename(filename);
        return contentType.getMimeType().startsWith("image/");
    }
    
    public List<String> getSupportedImageExtensions() {
        return Arrays.asList(ContentType.IMAGE_JPEG, ContentType.IMAGE_PNG, 
                           ContentType.IMAGE_GIF, ContentType.IMAGE_WEBP)
                .stream()
                .flatMap(type -> type.getExtensions().stream())
                .collect(Collectors.toList());
    }
}
```

## 📁 模块结构

```
itsuka-core/
├── src/main/java/org/itsuka/core/
│   ├── constant/
│   │   └── Constants.java              # 通用常量定义
│   ├── domain/
│   │   ├── Response.java               # 统一API响应格式
│   │   ├── Result.java                 # 业务结果包装类
│   │   ├── LoginUser.java              # 用户认证实体
│   │   └── AnonymousLoginUser.java     # 匿名用户实体
│   ├── enums/
│   │   └── ContentType.java            # 文件MIME类型枚举
│   ├── exception/
│   │   ├── ServiceException.java       # 业务异常基类
│   │   └── AuthenticationException.java # 认证异常
│   └── utils/
│       ├── Assert.java                 # 断言工具类
│       └── ReflectionKit.java          # 反射工具类
└── build.gradle                        # 模块构建配置
```

## ⚙️ 核心组件

### Response<T> - API响应封装
- **字段**: code, success, message, data, timestamp, requestId
- **静态方法**: success(), error(), 链式调用支持
- **支持场景**: REST API响应、全局异常处理、链路追踪

### Result<T> - 业务结果封装
- **函数式支持**: map, flatMap, filter, onSuccess, onFailure
- **安全操作**: of(), condition(), recover()
- **类型转换**: toResponse(), getOrThrow()

### ReflectionKit - 反射工具
- **性能优化**: 字段和方法缓存
- **安全操作**: 自动处理访问权限
- **全面支持**: 类型检查、注解处理、泛型解析

### ContentType - 文件类型
- **广泛支持**: 70+种常用文件类型
- **智能识别**: 根据扩展名、文件名、MIME类型识别
- **性能优化**: 预构建缓存映射

## ⚠️ 注意事项

### 依赖要求
- **Java 17+** - 使用了现代Java特性
- **Spring Security** - LoginUser实现UserDetails接口
- **Jackson** - JSON序列化支持
- **Lombok** - 代码简化

### 使用建议
- **统一响应格式** - 所有API统一使用Response封装
- **业务逻辑封装** - 复杂业务使用Result进行函数式处理
- **参数校验** - 优先使用Assert进行参数校验
- **异常处理** - 优先使用ServiceException系列

### 性能考虑
- **反射缓存** - ReflectionKit会缓存字段和方法，需要时可清理缓存
- **ContentType查找** - 使用预构建映射，查找效率高
- **Result链式调用** - 避免过长的链式调用影响性能