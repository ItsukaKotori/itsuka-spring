# Itsuka Dict Spring Boot Starter

数据字典自动转换 `Starter`，用于在 `Controller` 返回 `Response` 时，自动将指定的字段值转换为对应的字典标签。

## 功能概览
- **自动字典转换**: 通过注解 `@Dict` 自动将实体类中的字段（如状态码、类型值）转换为可读的文本标签。
- **多种数据源**: 支持从数据库、Redis 或静态 `Map` 中加载字典数据。
- **缓存支持**: 内置缓存机制，减少字典数据的重复加载，提升性能。

## 安装步骤
在 `build.gradle` 文件中添加依赖：

```gradle
dependencies {
    implementation project(':common:itsuka-dict-spring-boot-starter')
}
```

## 配置说明

在 `application.yml` 中配置字典的加载方式：

```yaml
itsuka:
  dict:
    # 配置数据来源，例如：db, redis, static
    loader-type: db
```

## 使用示例

1.  在实体类的字段上添加 `@Dict` 注解：

    ```java
    public class User {
        @Dict(type = "user_status")
        private Integer status;
    }
    ```

2.  `Controller` 方法正常返回包含该实体类的 `Response` 对象。

3.  返回的 `JSON` 中会自动添加 `statusText` 字段：

    ```json
    {
        "status": 1,
        "statusText": "正常"
    }
    ```

## 注意事项
- 使用前请确保已配置好字典数据的来源。
- `@Dict` 注解需要指定字典类型 `type`，与字典数据中的类型保持一致。