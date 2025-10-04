# Itsuka Web Spring Boot Starter

`Itsuka Web Spring Boot Starter` 提供了一系列 `Web` 开发相关的增强功能，包括统一的响应格式、全局异常处理和 `Swagger` 文档集成。

## 功能概览
- **统一响应格式**: 自动将 `Controller` 的返回结果包装成统一的 `JSON` 格式。
- **全局异常处理**: 捕获全局异常，并以统一的格式返回给前端。
- **Swagger 集成**: 自动配置 `Swagger`，快速生成 `API` 文档。
- **跨域支持**: 提供简单的跨域配置。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-web-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置 `Swagger` 和跨域：

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

itsuka:
  web:
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: "*"
      allowed-headers: "*"
```

## 使用示例

**1. Controller 示例**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public List<User> list() {
        // 业务逻辑
        return userService.list();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        // 业务逻辑
        return userService.getById(id);
    }
}
```

返回的 `JSON` 将被自动包装：

```json
{
  "code": 200,
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "admin"
    }
  ]
}
```

## 注意事项
- `Swagger` 的访问地址默认为 `/swagger-ui.html`。
- 跨域配置中的 `allowed-origins` 建议在生产环境中设置为具体的域名，而不是 `*`。