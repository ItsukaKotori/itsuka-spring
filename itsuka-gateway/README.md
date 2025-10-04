# Itsuka Gateway

基于 Spring Cloud Gateway 的统一 API 网关，提供路由、认证与限流能力。

## 功能概览
- 路由与负载均衡（系统服务与认证服务）
- JWT 认证（白名单路径可配置）
- 可选 Redis 限流（IP/用户维度）
- 熔断降级（`/fallback/system`、`/fallback/default`）
- Actuator 健康检查与路由信息

## 快速开始
- 开发：`./gradlew :itsuka-gateway:bootRun --args='--spring.profiles.active=dev'`
- 生产：
  - 构建：`./gradlew :itsuka-gateway:bootJar`
  - 启动：`java -jar itsuka-gateway/build/libs/itsuka-gateway-1.0.0.jar --spring.profiles.active=prod`

## 核心配置示例
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

itsuka:
  security:
    auth:
      excluded-paths:
        - /swagger-ui/**
        - /v3/api-docs/**
```

## 路由与端点
- 业务路由：`/api/system/**`、`/api/auth/**`
- 系统端点：`/actuator/health`、`/actuator/gateway/routes`
- 网关信息：`/gateway/info`
- 熔断：`/fallback/system`、`/fallback/default`

## 过滤器（简述）
- 日志：请求 ID、耗时记录
- 认证：JWT 校验与用户信息注入
- 限流：Redis 支持（可选）

## 注意事项
- Redis 仅用于限流与部分监控，非必需
- 目标后端默认 `http://localhost:8080`，按需调整

## 测试
- 运行：`./gradlew :itsuka-gateway:test`