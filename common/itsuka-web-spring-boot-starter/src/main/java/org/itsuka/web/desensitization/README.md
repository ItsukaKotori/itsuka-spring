# 字段脱敏功能

基于 Jackson 序列化实现的字段脱敏功能，支持多种常见脱敏规则和自定义脱敏规则。

## 功能特点

- 基于注解使用，简单方便
- 支持多种常见脱敏类型
- 支持自定义脱敏规则
- 基于 Jackson 序列化器实现，不影响业务逻辑
- 支持配置开关

## 脱敏类型

| 类型      | 说明         | 示例                                                       |
| --------- | ------------ | ---------------------------------------------------------- |
| USERNAME  | 用户名脱敏   | 张三丰 → 张**\*\***                                        |
| PHONE     | 手机号脱敏   | 13812345678 → 138\*\*\*\*5678                              |
| EMAIL     | 邮箱脱敏     | zhangsan@example.com → z**\*\***@example.com               |
| ID_CARD   | 身份证号脱敏 | 110105199003072345 → 110105**\*\*\*\***2345                |
| BANK_CARD | 银行卡号脱敏 | 6222021234567890123 → 622202**\*\*\*\***0123               |
| PASSWORD  | 密码脱敏     | password123 → ****\*\*\*****                               |
| ADDRESS   | 地址脱敏     | 北京市朝阳区建国门外大街 1 号 → 北京市******\*\*\*\******* |
| CUSTOM    | 自定义脱敏   | 可自定义保留前几位和后几位，以及替换字符                   |

## 使用方法

### 1. 在实体类字段上添加脱敏注解

```java
import org.itsuka.web.desensitization.Desensitize;
import org.itsuka.web.desensitization.DesensitizeType;

public class UserInfo {

    // 用户名脱敏
    @Desensitize(type = DesensitizeType.USERNAME)
    private String username;

    // 手机号脱敏
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    // 邮箱脱敏
    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;

    // 身份证号脱敏
    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;

    // 银行卡号脱敏
    @Desensitize(type = DesensitizeType.BANK_CARD)
    private String bankCard;

    // 密码脱敏
    @Desensitize(type = DesensitizeType.PASSWORD)
    private String password;

    // 地址脱敏
    @Desensitize(type = DesensitizeType.ADDRESS)
    private String address;

    // 自定义脱敏：保留前3位和后3位，中间用#代替
    @Desensitize(type = DesensitizeType.CUSTOM, prefixKeep = 3, suffixKeep = 3, replacer = '#')
    private String customField;
}
```

### 2. 配置开关

在 `application.yml` 中配置：

```yaml
itsuka:
  web:
    desensitization:
      enabled: true # 默认为true，可以省略
```

### 3. 使用示例

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public UserInfo getUser(@PathVariable Long id) {
        UserInfo user = userService.getById(id);
        // 返回时自动进行脱敏处理
        return user;
    }
}
```

## 自定义脱敏规则

对于 `CUSTOM` 类型，可以自定义以下参数：

- `prefixKeep`: 保留前几位，默认为 0
- `suffixKeep`: 保留后几位，默认为 0
- `replacer`: 替换字符，默认为'\*'

```java
// 保留前2位和后2位，中间用#代替
@Desensitize(type = DesensitizeType.CUSTOM, prefixKeep = 2, suffixKeep = 2, replacer = '#')
private String customField;
```

## 注意事项

1. 脱敏功能仅影响 JSON 序列化输出，不影响内存中的原始数据
2. 脱敏注解只能用于 String 类型的字段
3. 自定义脱敏时，如果保留的位数总和大于等于字符串长度，则不进行脱敏
4. 脱敏功能基于 Jackson 序列化器，确保项目中使用的是 Jackson 而不是其他 JSON 库

## 扩展

如果需要添加新的脱敏类型，可以：

1. 在 `DesensitizeType` 枚举中添加新类型
2. 在 `DesensitizationUtils` 中添加对应的脱敏方法
3. 在 `desensitize` 方法中添加对应的 case 分支
