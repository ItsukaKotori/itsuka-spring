# Itsuka-Spring

## 📖 项目介绍

Itsuka-Spring 是一个基于 Spring Boot 的企业级微服务开发框架，提供了一系列开箱即用的 Starter 组件，帮助开发者快速构建高质量的企业应用。

## 🏗️ 项目架构

### 📁 模块结构

```
itsuka-spring/
├── common/                           # 基础组件模块
│   ├── itsuka-core/                  # 核心工具库
│   ├── itsuka-web-spring-boot-starter/        # Web 功能增强
│   ├── itsuka-mybatis-plus-spring-boot-starter/ # MyBatis Plus 增强
│   ├── itsuka-redis-spring-boot-starter/       # Redis 集成
│   ├── itsuka-dict-spring-boot-starter/        # 字典翻译功能
│   ├── itsuka-file-spring-boot-starter/        # 文件存储管理
│   └── itsuka-mqtt-spring-boot-starter/        # MQTT 消息队列
└── itsuka-system/                    # 系统管理模块（示例应用）
```

### 🧩 核心组件

#### `itsuka-core` - 核心工具库
- 统一响应结构 (`Response<T>`) 和结果包装 (`Result<T>`)
- 用户认证实体 (`LoginUser`, `AnonymousLoginUser`)
- 异常体系 (`ServiceException`, `BusinessException`)
- 反射工具 (`ReflectionKit`) 支持注解处理和动态调用
- 内容类型枚举 (`ContentType`) 支持MIME类型管理
- IP工具 (`IpAddressToolkit`) 支持地址解析和脱敏

#### `itsuka-security-spring-boot-starter` - 安全认证
- JWT令牌管理 (`TokenProvider`) 支持生成、验证、刷新
- 访问控制 (`AccessControlManager`) 支持IP/路径/User-Agent白黑名单
- 认证过滤器 (`AuthenticationFilter`) 统一认证处理
- 用户上下文 (`SecurityContextManager`) 线程安全的用户信息管理
- 安全工具 (`SecurityToolkit`) 密码加密、权限检查等

#### `itsuka-web-spring-boot-starter` - Web功能增强
- 全局异常处理 (`GlobalExceptionHandler`) 统一错误响应
- 请求响应日志和性能监控拦截器
- Jackson配置优化和RestTemplate自动配置
- 支持业务异常、验证异常、HTTP异常、Spring Security异常

#### `itsuka-mybatis-plus-spring-boot-starter` - 数据库增强
- 实体基类体系：`BaseDomain` → `AuditableDomain` → `BusinessDomain` → `TenantDomain`
- 数据权限拦截器 (`DataPermissionInterceptor`) 基于创建人的权限控制
- 自动填充处理器 (`DefaultFieldHandler`) 创建时间、更新时间、操作人自动填充
- 自定义SQL注入器 (`CustomSqlInjector`) 扩展MyBatis-Plus内置方法
- 权限逃离器 (`DataPermissionEscaper`) 支持绕过数据权限检查

#### `itsuka-redis-spring-boot-starter` - Redis集成
- 智能缓存管理支持TTL随机化，防止缓存雪崩
- 数据库动态切换 (`@DbSwitch`) 支持Lettuce和Jedis客户端
- 响应式编程支持 (`ReactiveRedisTemplate`)
- 自动配置RedisTemplate、StringRedisTemplate、CacheManager

#### `itsuka-dict-spring-boot-starter` - 字典管理
- 智能字典转换 (`@DictField`, `@DictModel`) 支持覆盖原值或新增文本字段
- 多种缓存策略：本地缓存(ConcurrentHashMap)和Redis缓存
- 数据源自动加载支持MySQL、Oracle、PostgreSQL等数据库
- 响应拦截处理支持复杂对象、集合、Map等类型的递归处理

#### `itsuka-file-spring-boot-starter` - 文件存储
- MinIO对象存储集成支持文件上传、下载、删除
- 存储桶管理和对象列表功能
- 预签名链接生成支持安全的文件访问控制
- 自动配置MinIO客户端和参数校验

#### `itsuka-mqtt-spring-boot-starter` - MQTT消息队列
- 基于Netty的高性能MQTT服务器和客户端
- 事件驱动架构 (`MqttEventPublisher`) 支持Spring事件机制
- 会话管理 (`MqttSessionManager`) 和连接管理
- 支持启用注解：`@EnableMqttServer`、`@EnableMqttClient`、`@EnableMqtt`

#### `itsuka-gateway` - API网关
- 基于Spring Cloud Gateway的统一API网关
- 过滤器链：全局日志 → 访问控制 → JWT认证 → 限流
- 集成itsuka-security-spring-boot-starter提供统一安全功能
- 支持熔断降级、请求追踪、性能监控

#### `itsuka-system` - 系统管理
- 企业级权限管理：用户、角色、资源管理
- JWT认证和Spring Security集成
- RESTful API设计支持CRUD操作和分页查询
- 完整的权限控制体系和数据权限支持

## 🚀 快速开始

### 环境要求

- Java 17+
- Gradle 8.0+
- Spring Boot 3.5.5

### 编译项目

```bash
# 完整编译（包含测试）
./gradlew clean build

# 跳过测试编译
./gradlew clean build -x test
```

### 运行系统管理模块

```bash
# 运行主应用
./gradlew :itsuka-system:bootRun

# 或者运行编译后的 JAR
java -jar itsuka-system/build/libs/itsuka-system-1.0.0.jar
```

## 🛠️ 技术栈

- **框架**: Spring Boot 3.5.5, Spring Security
- **框架**: Spring Boot 3.5.6, Spring Security 6.x
- **数据访问**: MyBatis Plus 3.5.7
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **消息队列**: MQTT (Netty 4.x)
- **对象存储**: MinIO
- **认证**: JWT (JSON Web Tokens)
- **网关**: Spring Cloud Gateway 4.1.6
- **API文档**: SpringDoc OpenAPI 3.x
- **构建工具**: Gradle 8.0+
- **代码简化**: Lombok, MapStruct
- **测试**: JUnit 5, Testcontainers

## 📦 使用方式

### 引入 Starter

在您的 Spring Boot 项目中引入需要的 Starter：

```gradle
dependencies {
    implementation project(':common:itsuka-core')
    implementation project(':common:itsuka-web-spring-boot-starter')
    implementation project(':common:itsuka-mybatis-plus-spring-boot-starter')
    implementation project(':common:itsuka-redis-spring-boot-starter')
    implementation project(':common:itsuka-security-spring-boot-starter')
    // ... 其他需要的 starter
}
```

### 配置文件

```yaml
# application.yml 示例配置
itsuka:
  security:
    jwt:
      secret: your-jwt-secret-key-must-be-at-least-256-bits
      expiration: 24h
      issuer: your-application
    auth:
      enabled: true
      excluded-paths:
        - "/api/auth/**"      # 认证相关接口
        - "/actuator/**"      # 监控端点
        - "/swagger-ui/**"    # API文档
        - "/v3/api-docs/**"   # OpenAPI规范
    access-control:
      enabled: true
      blacklist:
        enabled: true
        user-agents:
          - "*bot*"
          - "*crawler*"
  dict:
    enabled: true
    cache-type: redis
  mqtt:
    server:
      enabled: true
      port: 1883
    client:
      enabled: true
      host: localhost
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🎯 版本信息

- **当前版本**: 1.0.0
- **Java 版本**: 17
- **Spring Boot 版本**: 3.5.6
- **Spring Cloud Gateway**: 4.1.6
- **MyBatis Plus**: 3.5.7

## 📚 重构历程

### 🔧 架构优化

#### 安全模块集中化
- **统一安全组件**: 将网关访问控制功能集成到 `itsuka-security-spring-boot-starter`
- **配置标准化**: 统一使用 `itsuka.security.*` 配置前缀
- **提升复用性**: 其他微服务可直接使用统一的安全功能

#### 代码质量提升
- **响应生成优化**: 创建 `GatewayResponseUtils` 工具类，消除101行重复代码
- **IP工具提取**: 将IP地址处理逻辑提取为 `IpAddressToolkit`，增强内网检测和脱敏功能
- **包命名规范**: 遵循Starter模块规范，使用 `manager`、`toolkit` 等标准包名

#### 配置元数据完善
- **IDE支持优化**: 添加Spring Boot配置处理器，自动生成配置元数据
- **开发体验**: VSCode等IDE可正确识别和提示所有配置属性
- **类型安全**: 提供完整的配置属性类型信息和默认值

### 🧹 代码清理
- **网关模块**: 清理291行无用代码（SystemServiceClient、FallbackController等）
- **目录优化**: 删除空目录，简化项目结构
- **文档整合**: 统一项目文档结构，确保每个模块只有一个README文档

