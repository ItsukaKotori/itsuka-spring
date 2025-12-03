package org.itsuka.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 邮件配置属性类
 * 支持主流邮箱服务提供商的配置
 *
 * @author Itsuka
 * @since 2023/12/03
 */
@Data
@ConfigurationProperties(prefix = "itsuka.mail")
public class MailProperties {
    
    /**
     * 是否启用邮件服务
     */
    private boolean enabled = true;
    
    /**
     * 默认发件人邮箱
     */
    private String from;
    
    /**
     * 默认发件人名称
     */
    private String fromName;
    
    /**
     * 邮件服务提供商类型
     */
    private MailProvider provider = MailProvider.GENERIC;
    
    /**
     * SMTP服务器配置
     */
    private SmtpProperties smtp = new SmtpProperties();
    
    /**
     * 模板配置
     */
    private TemplateProperties template = new TemplateProperties();
    
    /**
     * 自定义属性
     */
    private Map<String, String> customProperties;
    
    /**
     * 邮件服务提供商枚举
     */
    public enum MailProvider {
        /**
         * 通用SMTP配置
         */
        GENERIC,
        
        /**
         * Gmail
         */
        GMAIL,
        
        /**
         * Outlook/Hotmail
         */
        OUTLOOK,
        
        /**
         * QQ邮箱
         */
        QQ,
        
        /**
         * 163邮箱
         */
        NETEASE_163,
        
        /**
         * 126邮箱
         */
        NETEASE_126,
        
        /**
         * 企业微信邮箱
         */
        WECHAT_WORK,
        
        /**
         * 阿里云邮箱
         */
        ALIYUN
    }
    
    /**
     * SMTP配置
     */
    @Data
    public static class SmtpProperties {
        /**
         * SMTP服务器地址
         */
        private String host;
        
        /**
         * SMTP服务器端口
         */
        private int port = 587;
        
        /**
         * 是否使用SSL
         */
        private boolean ssl = false;
        
        /**
         * 是否使用TLS
         */
        private boolean tls = true;
        
        /**
         * 用户名
         */
        private String username;
        
        /**
         * 密码
         */
        private String password;
        
        /**
         * 授权码（用于QQ邮箱等）
         */
        private String authorizationCode;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 10000;
        
        /**
         * 读取超时时间（毫秒）
         */
        private int timeout = 10000;
        
        /**
         * 写入超时时间（毫秒）
         */
        private int writeTimeout = 10000;
        
        /**
         * 获取认证密码，优先使用授权码
         */
        public String getAuthenticationPassword() {
            return authorizationCode != null && !authorizationCode.isEmpty() ? authorizationCode : password;
        }
    }
    
    /**
     * 模板配置
     */
    @Data
    public static class TemplateProperties {
        /**
         * 模板引擎类型
         */
        private TemplateEngine engine = TemplateEngine.FREEMARKER;
        
        /**
         * 模板文件路径前缀
         */
        private String prefix = "classpath:/templates/mail/";
        
        /**
         * 模板文件后缀
         */
        private String suffix = ".ftl";
        
        /**
         * 模板编码
         */
        private String encoding = "UTF-8";
        
        /**
         * 模板引擎类型枚举
         */
        public enum TemplateEngine {
            FREEMARKER, THYMELEAF, VELOCITY
        }
    }
    
    /**
     * 获取有效的发件人地址
     */
    public String getEffectiveFrom() {
        return from != null && !from.trim().isEmpty() ? from : null;
    }
    
    /**
     * 获取有效的发件人名称
     */
    public String getEffectiveFromName() {
        return fromName != null && !fromName.trim().isEmpty() ? fromName : null;
    }
}