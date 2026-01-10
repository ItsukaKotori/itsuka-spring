# itsuka-web-spring-boot-starter

Web 相关的 Spring Boot Starter，提供了 Web 开发中常用的功能组件，包括脱敏、异常处理、性能监控等。

## 功能特性

- 数据脱敏：基于 Jackson 序列化实现的数据脱敏功能
- 全局异常处理：统一的异常处理机制
- 性能监控：API 性能监控和统计
- 请求响应日志：请求和响应日志记录
- RestTemplate 增强：增强的 RestTemplate 功能

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.itsuka</groupId>
    <artifactId>itsuka-web-spring-boot-starter</artifactId>
    <version>${itsuka.version}</version>
</dependency>
```

### 2. 配置文件

在`application.yml`中添加配置：

```yaml
itsuka:
  web:
    # Web功能总开关
    enable: true
    # 脱敏配置
    desensitization:
      enabled: true
      replacer: "*"
      default-prefix-keep: 0
      default-suffix-keep: 0
      log-desensitization: false
      error-desensitization: false
    # 日志配置
    logging:
      enabled: false
    # 性能监控配置
    performance:
      enabled: false
```

## 数据脱敏功能

### 概述

数据脱敏功能基于 Jackson 序列化实现，当对象被序列化为 JSON 时，会自动对带有`@Desensitization`注解的字段进行脱敏处理。

### 使用方式

1. 在需要脱敏的字段上添加`@Desensitization`注解：

```java
public class UserInfo {
    @Desensitization(type = DesensitizationType.CHINESE_NAME)
    private String name;

    @Desensitization(type = DesensitizationType.PHONE)
    private String phone;

    @Desensitization(type = DesensitizationType.EMAIL)
    private String email;

    @Desensitization(type = DesensitizationType.CUSTOM, prefixKeep = 3, suffixKeep = 4)
    private String idCard;

    // getters and setters
}
```

2. 在 Controller 中返回对象：

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public UserInfo getUser(@PathVariable Long id) {
        UserInfo user = userService.getById(id);
        return user; // 自动进行脱敏处理
    }
}
```

### 脱敏类型

系统内置了多种脱敏类型：

| 类型         | 说明                               | 示例                                                 |
| ------------ | ---------------------------------- | ---------------------------------------------------- |
| USERNAME     | 用户名脱敏，保留前 2 位和后 1 位   | 张三 -> 张\*                                         |
| CHINESE_NAME | 中文姓名脱敏，保留前 1 位          | 张三 -> 张\*                                         |
| ID_CARD      | 身份证号脱敏，保留前 6 位和后 4 位 | 110101199001011234 -> 110101**\*\*\*\***1234         |
| PHONE        | 手机号脱敏，保留前 3 位和后 4 位   | 13812345678 -> 138\*\*\*\*5678                       |
| FIXED_PHONE  | 固定电话脱敏，保留前 4 位和后 2 位 | 010-12345678 -> 010-**\***78                         |
| EMAIL        | 邮箱脱敏，保留前 2 位和@后内容     | zhangsan@example.com -> zh**\*\***@example.com       |
| BANK_CARD    | 银行卡号脱敏，保留前 4 位和后 4 位 | 6222020200012345678 -> 6222****\*\*\*\*****5678      |
| PASSWORD     | 密码脱敏，全部用\*代替             | password -> **\*\*\*\***                             |
| ADDRESS      | 地址脱敏，保留前 6 位              | 北京市朝阳区建国路 88 号 -> 北京市朝阳区**\*\*\*\*** |
| CUSTOM       | 自定义脱敏，需要指定保留的前后位数 | 根据参数自定义                                       |

### 注解参数

`@Desensitization`注解支持以下参数：

- `type`: 脱敏类型，默认为`CUSTOM`
- `prefixKeep`: 自定义脱敏时保留的前缀长度，默认为 0
- `suffixKeep`: 自定义脱敏时保留的后缀长度，默认为 0
- `replacer`: 脱敏替换字符，默认为`*`
- `enabled`: 是否启用脱敏，默认为`true`

### 配置参数

脱敏功能支持以下全局配置：

```yaml
itsuka:
  web:
    desensitization:
      enabled: true # 是否启用脱敏功能
      replacer: "*" # 默认替换字符
      default-prefix-keep: 0 # 默认保留前缀长度
      default-suffix-keep: 0 # 默认保留后缀长度
      log-desensitization: false # 是否对日志输出也进行脱敏
      error-desensitization: false # 是否对错误信息中的敏感数据进行脱敏
```

## 实现原理

脱敏功能基于 Jackson 的`ContextualSerializer`实现：

1. 当 Spring Boot 启动时，`DesensitizationAutoConfiguration`会自动注册`DesensitizationModule`
2. `DesensitizationModule`向 Jackson 注册`DesensitizationSerializer`
3. 当序列化带有`@Desensitization`注解的字段时，`DesensitizationSerializer.createContextual()`方法会被调用
4. 该方法会检查字段的注解，并根据注解参数创建相应的序列化器实例
5. 在序列化时，会调用`DesensitizationUtils.desensitize()`方法进行实际的脱敏处理

相比之前的 AOP 实现，序列化方式有以下优势：

1. 性能更好：只在序列化时进行处理，避免了反射操作
2. 更精确：只对需要序列化的字段进行脱敏
3. 更灵活：支持嵌套对象的脱敏
4. 更简单：无需配置 AOP，只需添加注解即可

## 注意事项

1. 脱敏功能仅在 JSON 序列化时生效，不会修改原始对象
2. 如果需要在日志中也进行脱敏，请配置`log-desensitization: true`
3. 脱敏功能只对 String 类型的字段有效
4. 自定义脱敏类型时，请确保`prefixKeep + suffixKeep < 字符串长度`
