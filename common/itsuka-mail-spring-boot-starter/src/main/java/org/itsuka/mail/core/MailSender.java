package org.itsuka.mail.core;

import org.itsuka.mail.model.MailMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送器接口
 * 提供邮件发送的核心功能
 *
 * @author Itsuka
 * @since 2023/12/03
 */
public interface MailSender {
    
    /**
     * 发送简单文本邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    boolean sendText(String to, String subject, String content);
    
    /**
     * 发送简单HTML邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content HTML邮件内容
     * @return 发送结果
     */
    boolean sendHtml(String to, String subject, String content);
    
    /**
     * 发送邮件消息
     *
     * @param mailMessage 邮件消息
     * @return 发送结果
     */
    boolean send(MailMessage mailMessage);
    
    /**
     * 批量发送邮件
     *
     * @param mailMessages 邮件消息列表
     * @return 发送结果列表
     */
    List<Boolean> sendBatch(List<MailMessage> mailMessages);
    
    /**
     * 发送模板邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 发送结果
     */
    boolean sendTemplate(String to, String subject, String templateName, Object variables);
    
    /**
     * 异步发送简单文本邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<Boolean> sendTextAsync(String to, String subject, String content);
    
    /**
     * 异步发送HTML邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content HTML邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<Boolean> sendHtmlAsync(String to, String subject, String content);
    
    /**
     * 异步发送邮件消息
     *
     * @param mailMessage 邮件消息
     * @return 异步发送结果
     */
    CompletableFuture<Boolean> sendAsync(MailMessage mailMessage);
    
    /**
     * 异步批量发送邮件
     *
     * @param mailMessages 邮件消息列表
     * @return 异步发送结果列表
     */
    CompletableFuture<List<Boolean>> sendBatchAsync(List<MailMessage> mailMessages);
    
    /**
     * 异步发送模板邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 异步发送结果
     */
    CompletableFuture<Boolean> sendTemplateAsync(String to, String subject, String templateName, Object variables);
    
    /**
     * 验证邮件配置
     *
     * @return 验证结果
     */
    boolean validateConfiguration();
    
    /**
     * 测试邮件发送
     *
     * @param to 测试收件人邮箱
     * @return 测试结果
     */
    boolean testConnection(String to);
}