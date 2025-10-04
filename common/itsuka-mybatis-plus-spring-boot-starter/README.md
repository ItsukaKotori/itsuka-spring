# Itsuka Mybatis Plus Spring Boot Starter

`Itsuka Mybatis Plus Spring Boot Starter` 是一个基于 `Mybatis-Plus` 的增强工具包，旨在简化 `CRUD` 操作并提供更强大的功能。

## 功能概览
- **CRUD 增强**: 内置通用的 `Service` 和 `Mapper`，简化单表操作。
- **代码生成器**: 提供强大的代码生成功能，一键生成 `Mapper`、`Service`、`Entity` 等代码。
- **分页插件**: 无缝集成 `Mybatis-Plus` 的物理分页插件。
- **性能分析**: 集成性能分析插件，帮助开发者发现慢查询。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-mybatis-plus-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置数据源和 `Mybatis-Plus`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your-database
    username: your-username
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
```

## 使用示例

**1. 定义实体**

```java
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
}
```

**2. 定义 Mapper**

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

**3. 定义 Service**

```java
@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {
}
```

## 注意事项
- 确保已正确配置数据源信息。
- `mapper-locations` 应指向正确的 `XML` 文件路径。