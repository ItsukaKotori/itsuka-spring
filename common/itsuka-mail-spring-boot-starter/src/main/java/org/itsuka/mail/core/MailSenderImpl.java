package org.itsuka.mail.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mail.MailProperties;
import org.itsuka.mail.model.MailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 邮件发送器实现类
 * 提供邮件发送的具体实现
 *
 * @author Itsuka
 * @since 2023/12/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailSenderImpl implements MailSender {
    
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final Configuration freemarkerConfiguration;
    
    @Override
    public boolean sendText(String to, String subject, String content) {
        Assert.hasText(to, "收件人邮箱不能为空");
        Assert.hasText(subject, "邮件主题不能为空");
        Assert.hasText(content, "邮件内容不能为空");
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromAddress());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        
        return sendMailMessage(message);
    }
    
    @Override
    public boolean sendHtml(String to, String subject, String content) {
        Assert.hasText(to, "收件人邮箱不能为空");
        Assert.hasText(subject, "邮件主题不能为空");
        Assert.hasText(content, "邮件内容不能为空");
        
        MailMessage mailMessage = MailMessage.createHtml(to, subject, content);
        return send(mailMessage);
    }
    
    @Override
    public boolean send(MailMessage mailMessage) {
        Assert.notNull(mailMessage, "邮件消息不能为空");
        Assert.notEmpty(mailMessage.getTo(), "收件人列表不能为空");
        Assert.hasText(mailMessage.getSubject(), "邮件主题不能为空");
        Assert.hasText(mailMessage.getContent(), "邮件内容不能为空");
        
        try {
            // 处理模板邮件（如果需要）
            MailMessage processedMessage = processTemplateIfNeeded(mailMessage);
            
            // 创建并发送邮件
            MimeMessage mimeMessage = createMimeMessage(processedMessage);
            mailSender.send(mimeMessage);
            
            log.info("邮件发送成功: to={}, subject={}", mailMessage.getTo(), mailMessage.getSubject());
            return true;
            
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", mailMessage.getTo(), mailMessage.getSubject(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<Boolean> sendBatch(List<MailMessage> mailMessages) {
        if (CollectionUtils.isEmpty(mailMessages)) {
            return List.of();
        }
        
        log.info("开始批量发送邮件，数量: {}", mailMessages.size());
        
        return mailMessages.stream()
                .map(this::send)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean sendTemplate(String to, String subject, String templateName, Object variables) {
        try {
            String content = processTemplate(templateName, variables instanceof Map ? (Map<String, Object>) variables : Map.of("data", variables));
            return sendHtml(to, subject, content);
        } catch (Exception e) {
            log.error("模板邮件发送失败: to={}, template={}", to, templateName, e);
            return false;
        }
    }
    
    @Override
    @Async
    public CompletableFuture<Boolean> sendTextAsync(String to, String subject, String content) {
        return CompletableFuture.completedFuture(sendText(to, subject, content));
    }
    
    @Override
    @Async
    public CompletableFuture<Boolean> sendHtmlAsync(String to, String subject, String content) {
        return CompletableFuture.completedFuture(sendHtml(to, subject, content));
    }
    
    @Override
    @Async
    public CompletableFuture<Boolean> sendAsync(MailMessage mailMessage) {
        return CompletableFuture.completedFuture(send(mailMessage));
    }
    
    @Override
    @Async
    public CompletableFuture<List<Boolean>> sendBatchAsync(List<MailMessage> mailMessages) {
        return CompletableFuture.completedFuture(sendBatch(mailMessages));
    }
    
    @Override
    @Async
    public CompletableFuture<Boolean> sendTemplateAsync(String to, String subject, String templateName, Object variables) {
        return CompletableFuture.completedFuture(sendTemplate(to, subject, templateName, variables));
    }
    
    @Override
    public boolean validateConfiguration() {
        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
            sender.testConnection();
            log.info("邮件配置验证成功");
            return true;
        } catch (Exception e) {
            log.error("邮件配置验证失败", e);
            return false;
        }
    }
    
    @Override
    public boolean testConnection(String to) {
        try {
            String subject = "邮件服务测试";
            String content = "<h1>邮件服务测试</h1><p>这是一封测试邮件，如果您收到此邮件，说明邮件服务配置正常。</p>";
            return sendHtml(to, subject, content);
        } catch (Exception e) {
            log.error("邮件连接测试失败: to={}", to, e);
            return false;
        }
    }
    
    /**
     * 发送简单邮件消息
     */
    private boolean sendMailMessage(SimpleMailMessage message) {
        try {
            mailSender.send(message);
            log.info("简单邮件发送成功: to={}, subject={}", String.join(",", message.getTo()), message.getSubject());
            return true;
        } catch (MailException e) {
            log.error("简单邮件发送失败: to={}, subject={}", String.join(",", message.getTo()), message.getSubject(), e);
            return false;
        }
    }
    
    /**
     * 获取发件人地址
     */
    private String getFromAddress() {
        return getFromAddress(mailProperties.getEffectiveFrom(), mailProperties.getEffectiveFromName());
    }
    
    /**
     * 获取发件人地址
     */
    private String getFromAddress(String from, String fromName) {
        String address = StringUtils.hasText(from) ? from : mailProperties.getEffectiveFrom();
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException("发件人邮箱地址未配置");
        }
        
        String effectiveFromName = StringUtils.hasText(fromName) ? fromName : mailProperties.getEffectiveFromName();
        
        if (StringUtils.hasText(effectiveFromName)) {
            return String.format("%s <%s>", effectiveFromName, address);
        }
        
        return address;
    }
    
    /**
     * 判断是否为HTML内容
     */
    private boolean isHtmlContent(String contentType) {
        return MailMessage.ContentType.TEXT_HTML.equals(contentType);
    }
    
    /**
     * 处理模板邮件（如果需要）
     */
    private MailMessage processTemplateIfNeeded(MailMessage mailMessage) throws Exception {
        if (mailMessage.getTemplateName() != null && !mailMessage.getTemplateName().trim().isEmpty()) {
            String content = processTemplate(mailMessage.getTemplateName(), mailMessage.getTemplateVariables());
            
            // 创建新的邮件消息，保留其他属性但替换内容
            return MailMessage.builder()
                    .to(mailMessage.getTo())
                    .cc(mailMessage.getCc())
                    .bcc(mailMessage.getBcc())
                    .subject(mailMessage.getSubject())
                    .content(content)
                    .contentType(mailMessage.getContentType())
                    .from(mailMessage.getFrom())
                    .fromName(mailMessage.getFromName())
                    .replyTo(mailMessage.getReplyTo())
                    .attachments(mailMessage.getAttachments())
                    .inlineResources(mailMessage.getInlineResources())
                    .priority(mailMessage.getPriority())
                    .requestReceipt(mailMessage.getRequestReceipt())
                    .headers(mailMessage.getHeaders())
                    .messageId(mailMessage.getMessageId())
                    .createTime(mailMessage.getCreateTime())
                    .build();
        }
        
        return mailMessage;
    }
    
    /**
     * 创建MimeMessage
     */
    private MimeMessage createMimeMessage(MailMessage mailMessage) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, mailProperties.getTemplate().getEncoding());
        
        // 设置基本信息
        helper.setFrom(getFromAddress(mailMessage.getFrom(), mailMessage.getFromName()));
        helper.setTo(mailMessage.getTo().toArray(new String[0]));
        helper.setSubject(mailMessage.getSubject());
        helper.setText(mailMessage.getContent(), isHtmlContent(mailMessage.getContentType()));
        
        // 设置抄送
        if (!CollectionUtils.isEmpty(mailMessage.getCc())) {
            helper.setCc(mailMessage.getCc().toArray(new String[0]));
        }
        
        // 设置密送
        if (!CollectionUtils.isEmpty(mailMessage.getBcc())) {
            helper.setBcc(mailMessage.getBcc().toArray(new String[0]));
        }
        
        // 设置回复地址
        if (StringUtils.hasText(mailMessage.getReplyTo())) {
            helper.setReplyTo(mailMessage.getReplyTo());
        }
        
        // 设置优先级
        if (mailMessage.getPriority() != null && mailMessage.getPriority() != 3) {
            mimeMessage.setHeader("X-Priority", String.valueOf(mailMessage.getPriority()));
        }
        
        // 设置回执
        if (Boolean.TRUE.equals(mailMessage.getRequestReceipt())) {
            helper.setReturnTo(getFromAddress());
        }
        
        // 设置自定义头
        if (!CollectionUtils.isEmpty(mailMessage.getHeaders())) {
            mailMessage.getHeaders().forEach((key, value) -> {
                try {
                    mimeMessage.setHeader(key, value);
                } catch (Exception e) {
                    log.warn("设置邮件头失败: {} = {}", key, value, e);
                }
            });
        }
        
        // 添加附件
        if (!CollectionUtils.isEmpty(mailMessage.getAttachments())) {
            for (MailMessage.Attachment attachment : mailMessage.getAttachments()) {
                helper.addAttachment(attachment.getName(), attachment.toDataHandler());
            }
        }
        
        // 添加内嵌资源
        if (!CollectionUtils.isEmpty(mailMessage.getInlineResources())) {
            for (MailMessage.InlineResource resource : mailMessage.getInlineResources()) {
                helper.addInline(resource.getContentId(), resource.toDataHandler());
            }
        }
        
        return mimeMessage;
    }
    
    /**
     * 处理模板
     */
    private String processTemplate(String templateName, Map<String, Object> variables) throws IOException, TemplateException {
        String fullTemplateName = templateName.endsWith(mailProperties.getTemplate().getSuffix()) ? 
            templateName : templateName + mailProperties.getTemplate().getSuffix();
        
        Template template = freemarkerConfiguration.getTemplate(fullTemplateName);
        StringWriter writer = new StringWriter();
        
        template.process(variables, writer);
        
        return writer.toString();
    }
}