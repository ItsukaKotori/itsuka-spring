package org.itsuka.mail.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 邮件消息DTO
 * 封装邮件发送所需的所有信息
 *
 * @author Itsuka
 * @since 2023/12/03
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailMessage {
    
    /**
     * 收件人邮箱地址列表
     */
    private List<String> to;
    
    /**
     * 抄送人邮箱地址列表
     */
    private List<String> cc;
    
    /**
     * 密送人邮箱地址列表
     */
    private List<String> bcc;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件正文内容
     */
    private String content;
    
    /**
     * 邮件类型：text纯文本，htmlHTML格式
     */
    private String contentType = ContentType.TEXT_HTML;
    
    /**
     * 发件人邮箱地址（可选，默认使用配置中的发件人）
     */
    private String from;
    
    /**
     * 发件人名称（可选）
     */
    private String fromName;
    
    /**
     * 回复邮箱地址（可选）
     */
    private String replyTo;
    
    /**
     * 附件列表
     */
    private List<Attachment> attachments;
    
    /**
     * 内嵌资源列表（用于HTML邮件中的图片等）
     */
    private List<InlineResource> inlineResources;
    
    /**
     * 模板变量（用于模板邮件）
     */
    private Map<String, Object> templateVariables;
    
    /**
     * 模板名称（用于模板邮件）
     */
    private String templateName;
    
    /**
     * 邮件优先级：1=高，3=普通，5=低
     */
    private Integer priority = 3;
    
    /**
     * 是否需要回执
     */
    private Boolean requestReceipt = false;
    
    /**
     * 自定义邮件头
     */
    private Map<String, String> headers;
    
    
    /**
     * 邮件附件类
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attachment {
        /**
         * 附件名称
         */
        private String name;
        
        /**
         * 附件数据
         */
        private byte[] data;
        
        /**
         * 附件MIME类型
         */
        private String contentType;
        
        /**
         * 附件输入流（与data二选一）
         */
        private InputStream inputStream;
    }
    
    /**
     * 内嵌资源类（用于HTML邮件中的图片等）
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InlineResource {
        /**
         * 资源ID（用于HTML中的cid引用）
         */
        private String contentId;
        
        /**
         * 资源数据
         */
        private byte[] data;
        
        /**
         * 资源MIME类型
         */
        private String contentType;
        
        /**
         * 资源输入流（与data二选一）
         */
        private InputStream inputStream;
    }
    
    /**
     * 邮件内容类型常量
     */
    public static class ContentType {
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
    }
}