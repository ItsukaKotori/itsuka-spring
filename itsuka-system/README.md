# itsuka-system

基于Spring Boot的企业级系统管理模块，提供完整的用户权限管理体系和认证授权功能。

## 🎯 核心功能

### 👥 用户管理
- 用户CRUD操作（创建、查询、更新、删除）
- 用户分页查询和条件筛选
- 密码加密存储（BCrypt）
- 用户状态管理
- 用户组织机构关联

### 🔐 角色权限管理
- 角色CRUD操作
- 角色权限分配
- 用户角色关联管理
- 资源权限控制

### 📋 资源管理
- 菜单资源管理
- 按钮权限管理
- 资源树形结构
- 角色资源绑定

### 🏢 组织机构管理
- 组织机构CRUD操作
- 树形层级结构管理（公司-部门-子部门-岗位）
- 组织机构移动和排序
- 部门用户关联查询
- 组织机构编码唯一性校验

### 🔑 认证授权
- JWT令牌认证
- 用户登录/登出
- 令牌刷新
- Spring Security集成

### 📊 系统监控
- Actuator健康检查
- 应用性能指标
- 系统运行状态

## 🚀 快速开始

### 1. 环境准备
- **JDK 17+**
- **MySQL 8.0+** 
- **Redis 6.0+**

### 2. 数据库初始化

执行项目根目录下的 `sql/itsuka.sql` 脚本，初始化数据库。

### 3. 配置说明

在 `application.yml` 中配置数据库、Redis等：

```yaml
spring:
  application:
    name: itsuka-system
  datasource:
    url: jdbc:mysql://localhost:3306/itsuka
    username: your-username
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# MyBatis Plus配置
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# 安全配置
itsuka:
  security:
    jwt:
      secret: your-jwt-secret-key-must-be-at-least-256-bits
      expiration: 24h
    auth:
      enabled: true
      excluded-paths:
        - "/api/auth/**"
        - "/actuator/**"
        - "/swagger-ui/**"
        - "/v3/api-docs/**"
```

### 4. 启动应用

```bash
# 开发环境
./gradlew :itsuka-system:bootRun

# 生产环境
./gradlew :itsuka-system:bootJar
java -jar itsuka-system/build/libs/itsuka-system-1.0.0.jar
```

## 📚 API文档

应用启动后，访问 `http://localhost:8080/swagger-ui.html` 查看API文档。

### 主要接口

#### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新令牌
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/current` - 获取当前用户信息

#### 用户管理
- `GET /user/page` - 用户分页查询
- `POST /user` - 创建用户
- `PUT /user` - 更新用户
- `DELETE /user/{id}` - 删除用户

#### 角色管理
- `GET /api/role/page` - 角色分页查询
- `GET /api/role/{id}` - 获取角色详情
- `POST /api/role` - 创建角色
- `PUT /api/role` - 更新角色
- `DELETE /api/role/{id}` - 删除角色

#### 资源管理
- `GET /api/resource` - 获取资源列表
- `GET /api/resource/tree` - 获取资源树
- `POST /api/resource` - 创建资源
- `PUT /api/resource` - 更新资源
- `DELETE /api/resource/{id}` - 删除资源
- `GET /api/resource/role/{roleId}` - 获取角色资源

#### 组织机构管理
- `GET /api/organization/tree` - 获取组织机构树
- `GET /api/organization/page` - 组织机构分页查询
- `GET /api/organization/children/{parentId}` - 获取子节点
- `GET /api/organization/type/{type}` - 根据类型查询组织机构
- `GET /api/organization/{id}` - 获取组织机构详情
- `POST /api/organization` - 创建组织机构
- `PUT /api/organization` - 更新组织机构
- `DELETE /api/organization/{id}` - 删除组织机构
- `PUT /api/organization/move/{id}/{newParentId}` - 移动组织机构
- `GET /api/organization/{id}/check-code/{code}` - 检查编码是否存在
- `GET /api/organization/{id}/users` - 获取部门下的用户列表

## ⚠️ 注意事项

### 配置要求
- 启动前请确保已正确配置 `application.yml` 中的数据库连接信息
- Redis服务必须正常运行，用于缓存和会话管理
- JWT密钥必须设置为至少256位的安全密钥

### 默认账户
- 管理员账户：`admin`
- 默认密码：`123456`
- 生产环境请及时修改默认密码

### 安全考虑
- 密码采用BCrypt加密存储
- JWT令牌有效期默认24小时，可根据需要调整
- 生产环境建议启用HTTPS
- 建议使用强密码策略

### 数据库设计
- 使用MyBatis-Plus作为ORM框架
- 支持逻辑删除，删除操作不会物理删除数据
- 自动填充创建时间、更新时间等字段
- 主键采用雪花算法生成

### 性能优化
- 数据库连接池配置适当的最大连接数
- Redis缓存合理使用，避免缓存雪崩
- 分页查询设置合理的页面大小限制
- 对高频查询建议添加数据库索引