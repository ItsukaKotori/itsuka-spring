# Itsuka Mail Spring Boot Starter

一个简洁高效的Spring Boot邮件发送Starter，专注于核心邮件发送功能，支持主流邮箱服务提供商。

## 功能特性

- 🚀 **开箱即用**：基于Spring Boot自动配置，简单配置即可使用
- 📧 **主流邮箱支持**：预配置Gmail、Outlook、QQ邮箱、163邮箱等主流邮箱服务
- 🔄 **异步发送**：支持异步邮件发送，提升应用性能
- 📝 **模板支持**：可选集成FreeMarker模板引擎（需要时启用）
- 📎 **附件支持**：支持发送附件和内嵌资源
- 🛡️ **SSL/TLS安全**：支持SSL和TLS加密传输
- 📊 **批量发送**：支持批量邮件发送
- 💡 **核心功能**：专注SMTP发送，移除非必需依赖

## 快速开始

### 1. 添加依赖

在你的Spring Boot项目中添加以下依赖：

```xml
<dependency>
    <groupId>org.itsuka</groupId>
    <artifactId>itsuka-mail-spring-boot-starter</artifactId>
    <version>${itsuka.version}</version>
</dependency>
```

### 2. 配置邮箱

在`application.yml`中配置邮箱信息：

```yaml
itsuka:
  mail:
    enabled: true
    provider: QQ
    from: your-email@qq.com
    from-name: Your Name
    smtp:
      username: your-email@qq.com
      authorization-code: your-authorization-code
```

### 3. 使用邮件服务

```java
@Autowired
private MailSender mailSender;

mailSender.sendText("recipient@example.com", "测试主题", "测试内容");
mailSender.sendHtml("recipient@example.com", "HTML测试", "<h1>Hello</h1><p>这是一封HTML邮件</p>");
mailSender.sendTextAsync("recipient@example.com", "异步测试", "异步发送内容");
```

## 配置说明

### 基础配置

```yaml
itsuka:
  mail:
    enabled: true
    from: sender@example.com
    from-name: Sender Name
    provider: GENERIC
```

### SMTP配置

```yaml
itsuka:
  mail:
    smtp:
      host: smtp.example.com
      port: 587
      username: username
      password: password
      authorization-code: auth-code
      ssl: false
      tls: true
      connection-timeout: 10000
      timeout: 10000
      write-timeout: 10000
```

### 模板配置（可选）

如果需要使用模板功能，需要：
1. 添加FreeMarker依赖到项目中
2. 启用模板配置

```yaml
itsuka:
  mail:
    template:
      enabled: true
      prefix: /templates/mail/
      suffix: .ftl
      encoding: UTF-8
```

## 支持的邮箱服务提供商

| 提供商 | 代码 | SMTP配置 |
|--------|------|----------|
| 通用SMTP | GENERIC | 自定义配置 |
| Gmail | GMAIL | smtp.gmail.com:587 (TLS) |
| Outlook | OUTLOOK | smtp-mail.outlook.com:587 (TLS) |
| QQ邮箱 | QQ | smtp.qq.com:587 (TLS) |
| 163邮箱 | NETEASE_163 | smtp.163.com:465 (SSL) |
| 126邮箱 | NETEASE_126 | smtp.126.com:465 (SSL) |
| 企业微信邮箱 | WECHAT_WORK | smtp.exmail.qq.com:587 (TLS) |
| 阿里云邮箱 | ALIYUN | smtp.mxhichina.com:587 (TLS) |

## 使用示例

### 发送带附件的邮件

```java
@Autowired
private MailSender mailSender;

MailMessage message = MailMessage.builder()
    .to(List.of("recipient@example.com"))
    .subject("带附件的邮件")
    .content("<h1>请查看附件</h1>")
    .contentType(MailMessage.ContentType.TEXT_HTML)
    .attachments(List.of(
        MailMessage.Attachment.builder()
            .name("document.pdf")
            .data(fileBytes)
            .contentType("application/pdf")
            .build()
    ))
    .build();

mailSender.send(message);
```

### 发送模板邮件

> **注意**：模板功能需要添加FreeMarker依赖并启用模板配置

```groovy
// build.gradle
implementation 'org.freemarker:freemarker:2.3.32'
```

```yaml
# application.yml
itsuka:
  mail:
    template:
      enabled: true
```

```java
@Autowired
private MailSender mailSender;

Map<String, Object> variables = Map.of("username", "张三");
mailSender.sendTemplate("user@example.com", "欢迎注册", "welcome", variables);
```

### 批量发送邮件

```java
List<MailMessage> messages = List.of(
    MailMessage.builder()
        .to(List.of("user1@example.com"))
        .subject("批量邮件1")
        .content("这是第一封邮件")
        .build(),
    MailMessage.builder()
        .to(List.of("user2@example.com"))
        .subject("批量邮件2")
        .content("这是第二封邮件")
        .build()
);

List<Boolean> results = mailSender.sendBatch(messages);
```

### 高级邮件配置

```java
MailMessage message = MailMessage.builder()
    .to(List.of("recipient@example.com"))
    .cc(List.of("cc@example.com"))
    .bcc(List.of("bcc@example.com"))
    .subject("高级邮件")
    .content("<h1>高级邮件内容</h1>")
    .contentType(MailMessage.ContentType.TEXT_HTML)
    .from("custom@example.com")
    .fromName("Custom Sender")
    .replyTo("reply@example.com")
    .priority(1)
    .requestReceipt(true)
    .headers(Map.of("X-Custom-Header", "CustomValue"))
    .build();

mailService.send(message);
```

## 常见邮箱配置示例

### QQ邮箱配置

```yaml
itsuka:
  mail:
    provider: QQ
    from: your-email@qq.com
    smtp:
      username: your-email@qq.com
      authorization-code: your-authorization-code
```

### Gmail配置

```yaml
itsuka:
  mail:
    provider: GMAIL
    from: your-email@gmail.com
    smtp:
      username: your-email@gmail.com
      password: your-app-password
```

### 163邮箱配置

```yaml
itsuka:
  mail:
    provider: NETEASE_163
    from: your-email@163.com
    smtp:
      username: your-email@163.com
      authorization-code: your-authorization-code
```

## 核心优化

### v2.0 优化说明

本版本专注核心邮件发送功能，进行了以下优化：

1. **移除非核心依赖**
   - 移除 `spring-boot-starter-actuator`（监控功能）
   - 移除 `spring-boot-starter-validation`（验证功能）
   - FreeMarker改为可选依赖（按需引入）

2. **简化配置项**
   - 移除POP3/IMAP配置（专注SMTP发送）
   - 简化模板引擎配置
   - 保留核心SMTP配置

3. **模板引擎可选化**
   - FreeMarker作为可选依赖
   - 需要时通过配置启用
   - 无模板需求时零依赖

4. **核心功能保留**
   - 文本/HTML邮件发送
   - 异步发送支持
   - 附件和内嵌资源
   - 批量发送
   - 主流邮箱预配置

## 注意事项

1. **安全性**：不要在代码中硬编码密码，使用配置文件或环境变量
2. **授权码**：QQ邮箱、163邮箱等需要使用授权码而不是密码
3. **SSL/TLS**：根据邮箱服务提供商的要求配置SSL或TLS
4. **模板功能**：需要时添加FreeMarker依赖并启用配置
5. **专注发送**：本Starter专注SMTP发送，不提供收件功能

## 问题排查

### 常见错误

1. **认证失败**：检查用户名和密码/授权码是否正确
2. **连接超时**：检查SMTP服务器地址和端口是否正确
3. **SSL/TLS错误**：检查SSL/TLS配置是否与邮箱服务提供商要求一致
4. **模板找不到**：检查模板文件路径和名称是否正确

### 调试模式

启用调试模式查看详细日志：

```yaml
logging:
  level:
    org.itsuka.mail: DEBUG
    org.springframework.mail: DEBUG
```

## 版本历史

- v2.0.0: 优化版本
  - 移除非核心依赖（actuator、validation）
  - FreeMarker改为可选依赖
  - 移除POP3/IMAP配置（专注SMTP发送）
  - 简化配置项和代码结构
- v1.3.0: 添加更多邮箱服务提供商预配置
- v1.2.0: 添加批量发送和附件支持
- v1.1.0: 添加异步发送和模板支持
- v1.0.0: 初始版本，支持基础邮件发送功能

## 许可证

本项目采用 MIT License 许可证。