# 网关访问控制功能

## 功能概述

网关访问控制功能提供了强大的白名单和黑名单机制，支持基于路径、IP地址和User-Agent的访问控制。

## 配置区别说明

### JWT认证排除路径 vs 访问控制

项目中有两个相关但不同的配置：

1. **JWT认证排除路径** (`app.gateway.auth.excluded-paths`)
   - 目的：跳过JWT令牌验证的路径
   - 场景：登录接口、健康检查、API文档等无需认证的公开接口
   - 处理：直接跳过认证过滤器，不验证JWT令牌

2. **访问控制** (`app.gateway.access-control`)
   - 目的：基于IP、User-Agent、路径进行访问控制
   - 场景：防止恶意访问、限制特定IP或User-Agent
   - 处理：在认证之前执行，可以拒绝访问

### 执行顺序
```
请求 → 访问控制过滤器 → JWT认证过滤器 → 业务逻辑
```

## 功能特性

### 1. 路径匹配
- 使用 Spring 的 `AntPathMatcher` 进行路径匹配
- 支持通配符模式：`*`、`**`、`?`
- 示例：
  - `/api/public/**` - 匹配所有public API
  - `/admin/*/config` - 匹配admin下任意单级目录的config
  - `/api/user?.json` - 匹配user1.json、userA.json等

### 2. IP地址匹配
- 支持单个IP地址：`192.168.1.100`
- 支持CIDR格式：`192.168.1.0/24`、`10.0.0.0/8`
- 支持通配符：`192.168.1.*`

### 3. User-Agent匹配
- 支持通配符模式匹配
- 示例：
  - `*Chrome*` - 匹配包含Chrome的User-Agent
  - `*bot*` - 匹配所有包含bot的爬虫

## 配置说明

### 基础配置

```yaml
app:
  gateway:
    access-control:
      enabled: true  # 是否启用访问控制
      
      whitelist:
        enabled: false  # 是否启用白名单
        paths:          # 白名单路径
          - "/api/public/**"
          - "/actuator/health"
        ips:            # 白名单IP
          - "127.0.0.1"
          - "192.168.1.0/24"
        user-agents:    # 白名单User-Agent
          - "*Chrome*"
          - "*Firefox*"
      
      blacklist:
        enabled: false  # 是否启用黑名单
        paths:          # 黑名单路径
          - "/api/admin/dangerous/**"
        ips:            # 黑名单IP
          - "192.168.1.100"
          - "10.0.0.0/8"
        user-agents:    # 黑名单User-Agent
          - "*bot*"
          - "*crawler*"
```

### 过滤器执行顺序

1. `GlobalLoggingFilter` (Ordered.HIGHEST_PRECEDENCE)
2. `AccessControlFilter` (Ordered.HIGHEST_PRECEDENCE + 1)
3. `AuthenticationFilter` (Ordered.HIGHEST_PRECEDENCE + 2)

## 访问控制逻辑

### 检查流程

1. **总开关检查**：如果 `access-control.enabled = false`，直接放行
2. **黑名单检查**：如果请求匹配黑名单任一条件，直接拒绝访问
3. **白名单检查**：如果启用白名单，请求必须匹配白名单条件才能通过

### 匹配规则

- **路径匹配**：使用AntPathMatcher，支持 `*`、`**`、`?` 通配符
- **IP匹配**：支持精确匹配、CIDR格式、通配符匹配
- **User-Agent匹配**：使用AntPathMatcher进行模式匹配

## 使用场景

### 1. 生产环境安全防护

```yaml
app:
  gateway:
    access-control:
      enabled: true
      blacklist:
        enabled: true
        paths:
          - "/api/admin/**"        # 阻止未授权的管理接口访问
          - "/api/internal/**"     # 阻止内部接口外部访问
        user-agents:
          - "*bot*"               # 阻止爬虫
          - "*spider*"
          - "*crawler*"
```

### 2. 特定功能白名单限制

```yaml
app:
  gateway:
    access-control:
      enabled: true
      whitelist:
        enabled: true
        paths:
          - "/api/payment/**"     # 支付接口仅允许特定来源
        ips:
          - "192.168.1.0/24"     # 仅允许内网访问
          - "10.0.0.100"         # 特定服务器
```

### 3. 开发环境调试

```yaml
app:
  gateway:
    access-control:
      enabled: true
      blacklist:
        enabled: true
        paths:
          - "/api/test/dangerous/**"  # 阻止危险测试接口
```

## 错误响应

当请求被访问控制拒绝时，返回403 Forbidden状态码和JSON响应：

```json
{
  "code": 403,
  "message": "Access denied",
  "timestamp": 1673616000000,
  "path": "/api/blocked/path"
}
```

## 注意事项

1. **性能考虑**：访问控制在认证之前执行，减少不必要的认证开销
2. **IP获取**：支持多种代理头获取真实IP（X-Forwarded-For、X-Real-IP等）
3. **CIDR支持**：完整支持IPv4 CIDR格式的网段匹配
4. **配置优先级**：黑名单优先于白名单执行
5. **日志记录**：所有访问控制决策都会记录日志便于调试

## 扩展建议

如果需要更复杂的访问控制逻辑，建议将此功能封装到独立的starter模块中：

- `itsuka-access-control-spring-boot-starter`
- 支持动态配置更新
- 支持更多匹配规则
- 支持访问控制统计和监控