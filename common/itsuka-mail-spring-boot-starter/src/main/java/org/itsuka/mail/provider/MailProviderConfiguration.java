package org.itsuka.mail.provider;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.mail.MailProperties;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.Map;

/**
 * 邮件服务提供商配置类
 * 提供主流邮箱服务的预配置
 *
 * @author Itsuka
 * @since 2023/12/03
 */
@Slf4j
public class MailProviderConfiguration {
    
    /**
     * 邮件服务提供商配置映射
     */
    private static final Map<MailProperties.MailProvider, MailProviderConfig> PROVIDER_CONFIG_MAP =
        new EnumMap<>(MailProperties.MailProvider.class);
    
    static {
        // 初始化各种邮件服务提供商的配置
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.GMAIL, new GmailProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.OUTLOOK, new OutlookProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.QQ, new QQProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.NETEASE_163, new Netease163ProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.NETEASE_126, new Netease126ProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.WECHAT_WORK, new WechatWorkProviderConfig());
        PROVIDER_CONFIG_MAP.put(MailProperties.MailProvider.ALIYUN, new AliyunProviderConfig());
    }
    
    /**
     * 根据提供商类型配置邮件属性
     *
     * @param properties 邮件属性
     */
    public static void configureByProvider(MailProperties properties) {
        if (properties.getProvider() == null || properties.getProvider() == MailProperties.MailProvider.GENERIC) {
            return;
        }
        
        log.info("配置邮件服务提供商: {}", properties.getProvider());
        
        MailProviderConfig config = PROVIDER_CONFIG_MAP.get(properties.getProvider());
        if (config != null) {
            config.configure(properties);
        } else {
            log.warn("不支持的邮件服务提供商: {}", properties.getProvider());
        }
    }
    
    /**
     * 邮件服务提供商配置接口
     */
    public interface MailProviderConfig {
        /**
         * 配置邮件属性
         *
         * @param properties 邮件属性
         */
        void configure(MailProperties properties);
        
        /**
         * 获取SMTP服务器配置
         */
        default ServerConfig getSmtpConfig() {
            return new ServerConfig();
        }
        
        /**
         * 获取POP3服务器配置
         */
        default ServerConfig getPop3Config() {
            return new ServerConfig();
        }
        
        /**
         * 获取IMAP服务器配置
         */
        default ServerConfig getImapConfig() {
            return new ServerConfig();
        }
    }
    
    /**
     * 服务器配置类
     */
    public static class ServerConfig {
        private String host;
        private int port;
        private boolean ssl;
        private boolean tls;
        
        public ServerConfig() {}
        
        public ServerConfig(String host, int port, boolean ssl, boolean tls) {
            this.host = host;
            this.port = port;
            this.ssl = ssl;
            this.tls = tls;
        }
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isSsl() { return ssl; }
        public void setSsl(boolean ssl) { this.ssl = ssl; }
        public boolean isTls() { return tls; }
        public void setTls(boolean tls) { this.tls = tls; }
    }
    
    /**
     * 抽象邮件服务提供商配置基类
     */
    public abstract static class AbstractMailProviderConfig implements MailProviderConfig {
        
        /**
         * 配置SMTP服务器属性
         */
        protected void configureSmtp(MailProperties properties, ServerConfig config) {
            MailProperties.SmtpProperties smtp = properties.getSmtp();
            if (!StringUtils.hasText(smtp.getHost())) {
                smtp.setHost(config.getHost());
            }
            if (smtp.getPort() == 587) {
                smtp.setPort(config.getPort());
            }
            smtp.setSsl(config.isSsl());
            smtp.setTls(config.isTls());
        }
        
        /**
         * 配置POP3服务器属性
         */
        protected void configurePop3(MailProperties properties, ServerConfig config) {
            MailProperties.Pop3Properties pop3 = properties.getPop3();
            if (!StringUtils.hasText(pop3.getHost())) {
                pop3.setHost(config.getHost());
            }
            if (pop3.getPort() == 995) {
                pop3.setPort(config.getPort());
            }
            pop3.setSsl(config.isSsl());
        }
        
        /**
         * 配置IMAP服务器属性
         */
        protected void configureImap(MailProperties properties, ServerConfig config) {
            MailProperties.ImapProperties imap = properties.getImap();
            if (!StringUtils.hasText(imap.getHost())) {
                imap.setHost(config.getHost());
            }
            if (imap.getPort() == 993) {
                imap.setPort(config.getPort());
            }
            imap.setSsl(config.isSsl());
        }
    }
    
    /**
     * Gmail配置
     */
    public static class GmailProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.gmail.com", 587, false, true));
            configurePop3(properties, new ServerConfig("pop.gmail.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.gmail.com", 993, true, false));
        }
    }
    
    /**
     * Outlook配置
     */
    public static class OutlookProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp-mail.outlook.com", 587, false, true));
            configurePop3(properties, new ServerConfig("pop-mail.outlook.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap-mail.outlook.com", 993, true, false));
        }
    }
    
    /**
     * QQ邮箱配置
     */
    public static class QQProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.qq.com", 587, false, true));
            configurePop3(properties, new ServerConfig("pop.qq.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.qq.com", 993, true, false));
        }
    }
    
    /**
     * 网易163邮箱配置
     */
    public static class Netease163ProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.163.com", 465, true, false));
            configurePop3(properties, new ServerConfig("pop.163.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.163.com", 993, true, false));
        }
    }
    
    /**
     * 网易126邮箱配置
     */
    public static class Netease126ProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.126.com", 465, true, false));
            configurePop3(properties, new ServerConfig("pop.126.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.126.com", 993, true, false));
        }
    }
    
    /**
     * 企业微信邮箱配置
     */
    public static class WechatWorkProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.exmail.qq.com", 587, false, true));
            configurePop3(properties, new ServerConfig("pop.exmail.qq.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.exmail.qq.com", 993, true, false));
        }
    }
    
    /**
     * 阿里云邮箱配置
     */
    public static class AliyunProviderConfig extends AbstractMailProviderConfig {
        @Override
        public void configure(MailProperties properties) {
            configureSmtp(properties, new ServerConfig("smtp.mxhichina.com", 587, false, true));
            configurePop3(properties, new ServerConfig("pop.mxhichina.com", 995, true, false));
            configureImap(properties, new ServerConfig("imap.mxhichina.com", 993, true, false));
        }
    }
}