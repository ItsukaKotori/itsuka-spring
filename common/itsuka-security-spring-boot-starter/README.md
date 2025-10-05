# Itsuka Security Spring Boot Starter

`Itsuka Security Spring Boot Starter` 是一个统一认证与安全管理的 `Starter`，旨在简化 `Spring Security` 的集成和使用，提供 JWT 认证、访问控制、IP 工具等完整的安全解决方案。

## 🚀 功能概览
- **JWT 支持**: 内置 `JWT` 的生成、解析和验证机制
- **访问控制**: 支持基于IP、User-Agent、路径的白名单/黑名单控制
- **自动化配置**: 自动配置 `Spring Security`，提供默认的安全策略
- **路径排除**: 支持自定义需要忽略安全认证的路径
- **认证上下文**: 提供便捷的工具类，用于获取当前登录用户信息
- **IP 工具集**: 提供IP地址获取、内网检测、脱敏等实用功能
- **令牌黑名单**: 支持JWT令牌黑名单管理
- **密码编码**: 集成BCrypt密码编码器

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-security-spring-boot-starter')
}
```

## 🛠️ 配置说明

在 `application.yml` 中配置安全相关的属性：

```yaml
itsuka:
  security:
    enabled: true
    jwt:
      secret: your-jwt-secret-key-must-be-at-least-256-bits
      expiration: 24h
      refresh-expiration: 7d
      issuer: your-application
      audience: your-app
    auth:
      enabled: true
      excluded-paths:
        - "/api/auth/**"
        - "/actuator/**"
        - "/swagger-ui/**"
        - "/v3/api-docs/**"
        - "/error"
    access-control:
      enabled: true
      whitelist:
        enabled: false
        paths:
          - "/api/public/**"
        ips:
          - "127.0.0.1"
          - "192.168.1.0/24"
        user-agents:
          - "*Chrome*"
      blacklist:
        enabled: true
        user-agents:
          - "*bot*"
          - "*crawler*"
```

## 💡 使用示例

### 1. 实现 UserDetailsProvider

你需要提供一个 `UserDetailsProvider` 的实现，用于根据用户名加载用户信息。

```java
@Component
public class MyUserDetailsProvider implements UserDetailsProvider {
    @Override
    public UserDetails loadUserByUsername(String username) {
        // 查询用户信息并返回
        return new SecurityUser(1L, username, "{noop}password", 
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
```

### 2. 登录接口

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
    
    @PostMapping("/logout")
    public Response<Void> logout(@RequestHeader("Authorization") String token) {
        authenticationHandler.logout(token);
        return Response.success();
    }
}
```

### 3. 使用IP工具类

```java
@Component
public class MyService {
    
    public void handleRequest(ServerHttpRequest request) {
        // 获取客户端真实IP
        String clientIp = IpAddressToolkit.getClientIp(request);
        
        // 检查是否为内网IP
        boolean isInternal = IpAddressToolkit.isInternalIp(clientIp);
        
        // 脱敏IP用于日志记录
        String safeIp = IpAddressToolkit.formatIpForLog(clientIp);
        log.info("Request from IP: {}", safeIp);
    }
}
```

### 4. 访问控制管理

```java
@Component
@RequiredArgsConstructor
public class SecurityService {
    private final AccessControlManager accessControlManager;
    
    public boolean checkAccess(String path, String ip, String userAgent) {
        return accessControlManager.isAccessAllowed(path, ip, userAgent);
    }
}
```

## 📁 模块结构

```
itsuka-security-spring-boot-starter/
├── src/main/java/com/itsuka/security/
│   ├── autoconfigure/
│   │   ├── SecurityAutoConfiguration.java      # 安全框架自动配置
│   │   ├── SecurityProperties.java            # 安全配置属性
│   │   └── AccessControlProperties.java       # 访问控制配置属性
│   ├── manager/
│   │   └── AccessControlManager.java          # 访问控制核心管理器
│   ├── token/
│   │   ├── TokenProvider.java                 # 令牌提供者接口
│   │   └── JwtTokenProvider.java              # JWT令牌实现
│   └── toolkit/
│       ├── IpAddressToolkit.java              # IP地址处理工具
│       ├── TokenBlacklistManager.java         # 令牌黑名单管理器
│       └── AuthenticationHandler.java         # 认证处理器（示例）
├── src/main/resources/META-INF/
│   ├── spring/
│   │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── spring-configuration-metadata.json # 自动生成的配置元数据
└── build.gradle                                # 模块构建配置
```

## ⚙️ 核心组件

### JWT 令牌管理
- **TokenProvider** - JWT令牌提供者接口，定义令牌操作规范
- **JwtTokenProvider** - JWT令牌默认实现，支持HMAC签名算法
- **TokenBlacklistManager** - 令牌黑名单管理器，支持内存缓存和定时清理

### 访问控制管理
- **AccessControlManager** - 访问控制核心管理器，支持多维度控制逻辑
- **AccessControlProperties** - 访问控制配置属性，支持白名单和黑名单配置
- **分类支持** - 路径/IP/User-Agent多维度控制，支持Ant风格匹配和CIDR网段

### 工具集
- **IpAddressToolkit** - IP地址处理工具集
  - 支持多种代理头获取客户端真实IP（X-Forwarded-For、X-Real-IP等）
  - 内网IP检测（支挖10.x.x.x、172.16-31.x.x、192.168.x.x等网段）
  - IP脱敏格式化（内网IP直接显示，公网IP脱敏处理）
- **BCryptPasswordEncoder** - 密码编码器，提供安全的密码加密
- **AntPathMatcher** - 路径匹配器，支持通配符模式

### 自动配置
- **SecurityAutoConfiguration** - 安全框架自动配置类
  - 条件化Bean注册，支持灵活启用/禁用
  - 配置属性绑定，支持IDE智能提示
  - 自动生成配置元数据，提供完整的IDE支持

### 示例组件
- **AuthenticationHandler** - 认证处理器示例（位于itsuka-system模块）
  - 登录/注销逻辑实现
  - 令牌刷新机制
  - 用户信息缓存（Redis）

## 🔧 配置属性说明

### JWT配置 (`itsuka.security.jwt.*`)
- `secret`: JWT签名密钥（必须至少256位）
- `expiration`: 令牌过期时间（默认24h）
- `refresh-expiration`: 刷新令牌过期时间（默认7d）
- `issuer`: 令牌发行者
- `audience`: 令牌受众
- `prefix`: 令牌前缀（默认"Bearer "）

### 认证配置 (`itsuka.security.auth.*`)
- `enabled`: 是否启用认证（默认true）
- `excluded-paths`: 认证排除路径列表
- `login-url`: 登录URL
- `logout-url`: 注销URL
- `unauthorized-url`: 未授权跳转URL

### 访问控制配置 (`itsuka.security.access-control.*`)
- `enabled`: 是否启用访问控制
- `whitelist.enabled`: 是否启用白名单
- `blacklist.enabled`: 是否启用黑名单
- 支持paths、ips、user-agents三个维度的控制

## ⚠️ 注意事项

### 安全要求
- **必需实现** - 务必提供 `UserDetailsService` 的实现，否则认证将无法工作
- **密钥安全** - `jwt.secret` 必须至少256位，生产环境应使用强密钥并定期轮换
- **HTTPS传输** - 生产环境中应使用HTTPS保证令牌传输安全
- **令牌过期** - 合理设置访问令牌和刷新令牌的过期时间

### 功能特性
- **访问控制优先级** - 黑名单优先于白名单执行，提供灵活的安全策略
- **最佳实践集成** - JWT认证排除路径与访问控制独立运行，互不干扰
- **多代理支持** - IP获取支持多种代理头，适用于复杂网络环境
- **CIDR网段** - 完整支持IPv4 CIDR格式的网段匹配
- **性能友好** - 内网IP检测优化，减少不必要的脱敏处理

### 性能与限制
- **黑名单管理** - 基于内存的令牌黑名单，重启后失效，大规模部署建议使用分布式缓存
- **路径匹配性能** - 使用AntPathMatcher，过多复杂路径规则可能影响性能
- **内存使用** - IP工具和访问控制优先内网检测，减少不必要的计算开销
- **缓存策略** - TokenBlacklistManager定时清理过期令牌，避免内存泄漏