# Itsuka Gateway

基于 Spring Cloud Gateway 的统一 API 网关，提供路由、认证、访问控制与限流能力。

## 🚀 功能概览
- **统一路由** - 系统服务与认证服务的路由与负载均衡
- **JWT 认证** - 基于 itsuka-security-spring-boot-starter 的统一认证
- **访问控制** - 支持基于IP、User-Agent、路径的白名单/黑名单控制
- **Redis 限流** - 可选的IP/用户维度限流功能
- **熔断降级** - 提供 `/fallback/system`、`/fallback/default` 降级端点
- **监控观测** - Actuator 健康检查与路由信息暴露
- **全局日志** - 请求追踪、性能监控与 Request-ID 支持

## 快速开始
- 开发：`./gradlew :itsuka-gateway:bootRun --args='--spring.profiles.active=dev'`
- 生产：
  - 构建：`./gradlew :itsuka-gateway:bootJar`
  - 启动：`java -jar itsuka-gateway/build/libs/itsuka-gateway-1.0.0.jar --spring.profiles.active=prod`

## 🛠️ 核心配置

### 基础配置
```yaml
server:
  port: 8888

spring:
  application:
    name: itsuka-gateway
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

itsuka:
  security:
    jwt:
      secret: your-jwt-secret-key-must-be-at-least-256-bits
      expiration: 24h
      issuer: itsuka-gateway
    auth:
      enabled: true
      excluded-paths:
        - "/api/auth/**"      # 认证相关接口
        - "/actuator/**"      # 监控端点
        - "/swagger-ui/**"    # API文档
        - "/v3/api-docs/**"   # OpenAPI规范
        - "/error"            # 错误页面

    # 访问控制配置（基于IP、User-Agent、路径的白名单/黑名单）
    access-control:
      enabled: true
      whitelist:
        enabled: false
        paths:
          - "/api/public/**"
          - "/actuator/health"
        ips:
          - "127.0.0.1"
          - "192.168.1.0/24"
        user-agents:
          - "*Chrome*"
          - "*Firefox*"
      blacklist:
        enabled: true
        paths:
          - "/api/admin/dangerous/**"
        ips:
          - "192.168.1.100"
          - "10.0.0.0/8"
        user-agents:
          - "*bot*"
          - "*crawler*"

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
```

## 🔌 路由与端点

### 业务路由
- **系统服务**: `/api/system/**` → `http://localhost:8080`
- **认证服务**: `/api/auth/**` → `http://localhost:8080`
- **API文档**: `/swagger-ui/**`, `/v3/api-docs/**` → `http://localhost:8080`

### 系统端点
- **健康检查**: `/actuator/health`
- **网关路由**: `/actuator/gateway/routes` 
- **网关信息**: `/gateway/info`
- **熔断降级**: `/fallback/system`, `/fallback/default`

## 🛡️ 安全特性

### JWT 认证
- 基于 `itsuka-security-spring-boot-starter` 统一认证
- 支持认证排除路径配置
- 自动用户信息注入与上下文管理

### 访问控制
集成的访问控制功能现已迁移到 `itsuka-security-spring-boot-starter`，提供：

#### 白名单控制
- **路径白名单**: 支持Ant风格路径匹配（如 `/api/public/**`）
- **IP白名单**: 支持CIDR格式（如 `192.168.1.0/24`）
- **User-Agent白名单**: 支持通配符匹配（如 `*Chrome*`）

#### 黑名单控制  
- **路径黑名单**: 阻止特定路径访问
- **IP黑名单**: 阻止特定IP或网段
- **User-Agent黑名单**: 阻止爬虫等（如 `*bot*`）

#### 执行顺序
```
请求 → 访问控制过滤器 → JWT认证过滤器 → 业务逻辑
```

### 限流功能
- **Redis支持**: 基于Redis的分布式限流
- **多维度**: 支持IP和用户维度限流
- **可选启用**: 非必需依赖，灵活配置

## 📊 过滤器架构

### 过滤器链（按执行顺序）
1. **GlobalLoggingFilter** - 全局日志过滤器
   - 请求追踪（Request-ID生成）
   - 性能监控（响应时间记录）
   - 请求/响应日志记录

2. **AccessControlFilter** - 访问控制过滤器  
   - IP地址检查
   - User-Agent验证
   - 路径权限控制
   - 基于 `AccessControlManager` 统一管理

3. **AuthenticationFilter** - JWT认证过滤器
   - JWT令牌验证
   - 用户信息解析与注入
   - 认证排除路径支持
   - 基于 `itsuka-security-spring-boot-starter`

4. **RateLimitFilter** - 限流过滤器（可选）
   - Redis分布式限流
   - IP/用户维度控制
   - 限流头部信息添加

### 响应工具统一化
所有过滤器使用 `GatewayResponseUtils` 统一生成响应：
- **401未授权响应**: `createUnauthorizedResponse()`
- **403禁止访问响应**: `createForbiddenResponse()`  
- **429限流响应**: `createRateLimitResponse()`
- **500服务器错误响应**: `createInternalServerErrorResponse()`

## ⚠️ 注意事项

### 依赖要求
- **Redis**: 仅用于限流与部分监控功能，非必需依赖
- **后端服务**: 目标后端默认 `http://localhost:8080`，生产环境需调整
- **JWT密钥**: 必须配置足够复杂的JWT密钥（至少256位）

### 配置说明
- **访问控制**: 黑名单优先于白名单执行
- **认证排除**: JWT认证排除路径与访问控制是独立的两个功能
- **IP获取**: 支持多种代理头获取真实IP（X-Forwarded-For、X-Real-IP等）
- **CIDR支持**: 完整支持IPv4 CIDR格式的网段匹配

### 错误响应格式
当请求被拒绝时，返回统一的JSON响应格式：

```json
{
  "code": 403,
  "message": "Access denied",
  "timestamp": 1673616000000,
  "path": "/api/blocked/path",
  "status": "Forbidden"
}
```

## 📁 项目结构

清理后的网关模块结构（已删除291行无用代码）：

```
src/main/java/com/itsuka/gateway/
├── GatewayApplication.java
├── filter/
│   ├── AccessControlFilter.java    # 访问控制过滤器
│   ├── AuthenticationFilter.java   # JWT认证过滤器
│   ├── GlobalLoggingFilter.java    # 全局日志过滤器
│   └── RateLimitFilter.java        # 限流过滤器
├── service/
│   └── UserCacheService.java       # 用户缓存服务
└── util/
    └── GatewayResponseUtils.java   # 响应工具类
```

### 已清理的无用类
- ❌ **SystemServiceClient** (216行) - 完全未被使用的服务客户端
- ❌ **FallbackController** (52行) - 无实际熔断器配置的控制器
- ❌ **WebClientConfig** (23行) - 仅被已删除类使用的配置

## 🧪 测试

```bash
# 运行单元测试
./gradlew :itsuka-gateway:test

# 运行集成测试
./gradlew :itsuka-gateway:integrationTest
```