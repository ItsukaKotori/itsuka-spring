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
- 统一响应结构 (`Response`)
- 用户认证实体 (`LoginUser`, `AnonymousLoginUser`)
- 异常体系 (`ServiceException`)
- 常量定义 (`Constants`)
- 算法工具 (队列、栈、缓存等数据结构实现)
- 反射工具 (`ReflectionKit`)

#### `itsuka-web-spring-boot-starter` - Web 功能增强
- 全局异常处理 (`GlobalExceptionHandler`)
- REST 客户端增强 (`CustomRestTemplate`)
- 文件下载工具 (`Downloader`)
- Spring 上下文工具 (`SpringContextHolder`)
- JSON 深拷贝工具 (`JacksonDeepCopyer`)

#### `itsuka-mybatis-plus-spring-boot-starter` - MyBatis Plus 增强
- 基础实体类 (`BaseDomain`)
- 自定义 SQL 注入器 (`CustomSqlInjector`)
- 数据权限拦截器 (`DataPermissionInterceptor`)
- 自动填充处理器 (`DefaultFieldHandler`)
- 自定义 Mapper (`CustomMapper`)

#### `itsuka-redis-spring-boot-starter` - Redis 集成
- 数据库切换注解 (`@DbSwitch`)
- 自定义 Redis 模板 (`CustomRedisTemplate`)
- 连接工厂管理 (`ConnectionFactoryHolder`)
- 缓存配置 (`RedisCacheProperties`)

#### `itsuka-dict-spring-boot-starter` - 字典翻译
- 字典注解支持
- 多数据库方言适配
- 缓存管理
- 自动翻译拦截器

#### `itsuka-file-spring-boot-starter` - 文件存储
- MinIO 对象存储集成
- 统一文件存储接口
- 文件上传下载功能

#### `itsuka-mqtt-spring-boot-starter` - MQTT 消息队列
- MQTT 服务器和客户端
- 事件驱动架构
- 会话管理
- 消息编解码

#### `itsuka-system` - 系统管理模块
- 用户管理 (`UserController`, `UserService`)
- 角色管理 (`RoleController`, `RoleService`)
- 资源管理 (`ResourceController`, `ResourceService`)
- JWT 认证 (`JwtAuthenticationFilter`, `JwtUtils`)
- Spring Security 配置

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
- **数据访问**: MyBatis Plus 3.5.7
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **消息队列**: MQTT (Netty)
- **对象存储**: MinIO
- **认证**: JWT
- **API 文档**: SpringDoc OpenAPI
- **构建工具**: Gradle 9.0
- **代码简化**: Lombok, MapStruct

## 📦 使用方式

### 引入 Starter

在您的 Spring Boot 项目中引入需要的 Starter：

```gradle
dependencies {
    implementation 'org.itsuka:itsuka-core:1.0.0'
    implementation 'org.itsuka:itsuka-web-spring-boot-starter:1.0.0'
    implementation 'org.itsuka:itsuka-mybatis-plus-spring-boot-starter:1.0.0'
    implementation 'org.itsuka:itsuka-redis-spring-boot-starter:1.0.0'
    // ... 其他需要的 starter
}
```

### 配置文件

```yaml
# application.yml 示例配置
itsuka:
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
- **Spring Boot 版本**: 3.5.5

