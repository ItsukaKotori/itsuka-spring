# Itsuka System

`Itsuka System` 是项目的核心业务模块，集成了用户管理、角色管理、菜单管理等基础功能。

## 功能概览
- **用户管理**: 提供用户的增、删、改、查功能。
- **角色管理**: 支持角色的权限分配和管理。
- **菜单管理**: 实现动态菜单的配置和展示。
- **系统监控**: 集成 `Actuator`，提供应用健康检查、性能指标等监控功能。

## 快速开始

**1. 环境准备**
- `JDK 17+`
- `MySQL 8.0+`
- `Redis`

**2. 数据库初始化**

执行项目根目录下的 `sql/itsuka.sql` 脚本，初始化数据库。

**3. 启动应用**

执行 `ItsukaSystemApplication` 的 `main` 方法，启动后台服务。

## 配置说明

在 `application.yml` 中配置数据库、`Redis` 等：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/itsuka
    username: your-username
    password: your-password
  redis:
    host: localhost
    port: 6379
```

## API 文档

应用启动后，访问 `http://localhost:8080/swagger-ui.html` 查看 `API` 文档。

## 注意事项
- 启动前请确保已正确配置 `application.yml` 中的各项参数。
- 默认的管理员账号为 `admin`，密码为 `123456`。