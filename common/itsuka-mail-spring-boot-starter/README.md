# Itsuka Mail Spring Boot Starter

一个简洁高效的Spring Boot邮件发送Starter，支持主流邮箱服务提供商，提供简单易用的邮件发送API。

## 功能特性

- 🚀 **开箱即用**：基于Spring Boot自动配置，简单配置即可使用
- 📧 **主流邮箱支持**：预配置Gmail、Outlook、QQ邮箱、163邮箱等主流邮箱服务
- 🔄 **异步发送**：支持异步邮件发送，提升应用性能
- 📝 **模板支持**：集成FreeMarker模板引擎，支持邮件模板
- 📎 **附件支持**：支持发送附件和内嵌资源
- 🛡️ **SSL/TLS安全**：支持SSL和TLS加密传输
- 📊 **批量发送**：支持批量邮件发送
- 🔧 **高度可扩展**：支持自定义邮件服务提供商和配置

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
private MailService mailService;

mailService.sendText("recipient@example.com", "测试主题", "测试内容");
mailService.sendHtml("recipient@example.com", "HTML测试", "<h1>Hello</h1><p>这是一封HTML邮件</p>");
mailService.sendTextAsync("recipient@example.com", "异步测试", "异步发送内容");
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

### 模板配置

```yaml
itsuka:
  mail:
    template:
      engine: FREEMARKER
      prefix: classpath:/templates/mail/
      suffix: .ftl
      encoding: UTF-8
```

## 支持的邮箱服务提供商

| 提供商 | 代码 | 说明 |
|--------|------|------|
| 通用SMTP | GENERIC | 自定义SMTP配置 |
| Gmail | GMAIL | Google Gmail |
| Outlook | OUTLOOK | Microsoft Outlook/Hotmail |
| QQ邮箱 | QQ | 腾讯QQ邮箱 |
| 163邮箱 | NETEASE_163 | 网易163邮箱 |
| 126邮箱 | NETEASE_126 | 网易126邮箱 |
| 企业微信邮箱 | WECHAT_WORK | 腾讯企业微信邮箱 |
| 阿里云邮箱 | ALIYUN | 阿里云邮箱 |

## 使用示例

### 发送带附件的邮件

```java
@Autowired
private MailService mailService;

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

mailService.send(message);
```

### 发送模板邮件

```java
@Autowired
private MailService mailService;

Map<String, Object> variables = Map.of("username", "张三");
mailService.sendTemplate("user@example.com", "欢迎注册", "welcome", variables);
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

List<Boolean> results = mailService.sendBatch(messages);
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

## 扩展开发

### 自定义邮件服务提供商

```java
@Component
public class CustomMailProvider implements MailProvider {
    
    @Override
    public void configure(MailProperties properties) {
        properties.getSmtp().setHost("custom.smtp.com");
        properties.getSmtp().setPort(587);
    }
}
```

### 自定义邮件服务

```java
@Service
@Primary
public class CustomMailService implements MailService {
    
    @Override
    public boolean send(MailMessage mailMessage) {
        return true;
    }
}
```

## 注意事项

1. **安全性**：不要在代码中硬编码密码，使用配置文件或环境变量
2. **授权码**：QQ邮箱、163邮箱等需要使用授权码而不是密码
3. **SSL/TLS**：根据邮箱服务提供商的要求配置SSL或TLS
4. **异步发送**：启用异步发送时，注意线程池配置
5. **模板路径**：确保模板文件路径正确，文件存在

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

- v2.0.0: 简化版本，移除复杂功能，保留核心邮件发送能力
- v1.3.0: 添加更多邮箱服务提供商预配置
- v1.2.0: 添加批量发送和附件支持
- v1.1.0: 添加异步发送和模板支持
- v1.0.0: 初始版本，支持基础邮件发送功能

## 许可证

本项目采用 MIT License 许可证。