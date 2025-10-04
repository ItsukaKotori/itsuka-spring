# Itsuka Security Spring Boot Starter

`Itsuka Security Spring Boot Starter` 是一个统一认证与 `JWT` 管理的 `Starter`，旨在简化 `Spring Security` 的集成和使用。

## 功能概览
- **JWT 支持**: 内置 `JWT` 的生成、解析和验证机制。
- **自动化配置**: 自动配置 `Spring Security`，提供默认的安全策略。
- **路径排除**: 支持自定义需要忽略安全认证的路径。
- **认证上下文**: 提供便捷的工具类，用于获取当前登录用户信息。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-security-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置安全相关的属性：

```yaml
itsuka:
  security:
    enabled: true
    jwt:
      secret: your-jwt-secret
      expiration: 86400 # 1 day
    auth:
      excluded-paths:
        - /api/auth/**
        - /swagger-ui.html
```

## 使用示例

**1. 实现 UserDetailsProvider**

你需要提供一个 `UserDetailsProvider` 的实现，用于根据用户名加载用户信息。

```java
@Component
public class MyUserDetailsProvider implements UserDetailsProvider {
    @Override
    public UserDetails loadUserByUsername(String username) {
        // a simple example
        return new SecurityUser(1L, username, "{noop}password", Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
```

**2. 登录接口**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationHandler authenticationHandler;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authenticationHandler.authenticate(request.getUsername(), request.getPassword());
    }
}
```

## 注意事项
- 务必提供 `UserDetailsProvider` 的实现，否则认证将无法工作。
- `jwt.secret` 必须是一个足够复杂的字符串，以保证安全性。