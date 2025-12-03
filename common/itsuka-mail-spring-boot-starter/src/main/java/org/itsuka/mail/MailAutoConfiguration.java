package org.itsuka.mail;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mail.provider.MailProviderConfiguration;
import org.itsuka.mail.core.MailSender;
import org.itsuka.mail.core.MailSenderImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 邮件自动配置类
 * 自动配置邮件发送相关的Bean
 *
 * @author Itsuka
 * @since 2023/12/03
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnProperty(prefix = "itsuka.mail", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MailProperties.class)
@Import({MailProviderConfiguration.class})
@EnableAsync
public class MailAutoConfiguration {
    
    /**
     * 配置JavaMailSender
     */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        log.info("初始化JavaMailSender");
        
        // 根据提供商配置邮件属性
        MailProviderConfiguration.configureByProvider(mailProperties);
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        applyProperties(mailSender, mailProperties);
        
        return mailSender;
    }
    
    /**
     * 配置邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public MailSender mailSender(JavaMailSender javaMailSender, MailProperties mailProperties,
                                 Configuration freemarkerConfiguration) {
        log.info("初始化MailSender");
        return new MailSenderImpl(javaMailSender, mailProperties, freemarkerConfiguration);
    }
    
    /**
     * 配置FreeMarker模板引擎
     */
    @Bean
    @ConditionalOnMissingBean(Configuration.class)
    public Configuration freemarkerConfiguration(MailProperties mailProperties) {
        log.info("初始化FreeMarker配置");
        
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setDefaultEncoding(mailProperties.getTemplate().getEncoding());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        
        try {
            configuration.setDirectoryForTemplateLoading(
                new java.io.File(mailProperties.getTemplate().getPrefix().replace("classpath:/", ""))
            );
        } catch (Exception e) {
            log.warn("无法设置模板目录，将使用类路径模板: {}", e.getMessage());
            configuration.setClassForTemplateLoading(this.getClass(), "/");
        }
        
        return configuration;
    }
    
    /**
     * 应用邮件配置属性
     */
    private void applyProperties(JavaMailSenderImpl mailSender, MailProperties mailProperties) {
        MailProperties.SmtpProperties smtp = mailProperties.getSmtp();
        
        // 基本配置
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setUsername(smtp.getUsername());
        
        // 密码配置：优先使用授权码，然后使用密码
        String password = smtp.getAuthenticationPassword();
        if (StringUtils.hasText(password)) {
            mailSender.setPassword(password);
        }
        
        // 协议配置
        mailSender.setProtocol("smtp");
        
        // 编码配置
        mailSender.setDefaultEncoding(mailProperties.getTemplate().getEncoding());
        
        // JavaMail属性配置
        Properties javaMailProperties = new Properties();
        
        // 认证配置
        javaMailProperties.put("mail.smtp.auth", "true");
        
        // SSL/TLS配置
        if (smtp.isSsl()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            javaMailProperties.put("mail.smtp.socketFactory.fallback", "false");
        } else if (smtp.isTls()) {
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
            javaMailProperties.put("mail.smtp.starttls.required", "true");
        }
        
        // 超时配置
        javaMailProperties.put("mail.smtp.connectiontimeout", smtp.getConnectionTimeout());
        javaMailProperties.put("mail.smtp.timeout", smtp.getTimeout());
        javaMailProperties.put("mail.smtp.writetimeout", smtp.getWriteTimeout());
        
        // 调试配置
        if (log.isDebugEnabled()) {
            javaMailProperties.put("mail.debug", "true");
        }
        
        // 自定义属性
        if (mailProperties.getCustomProperties() != null) {
            javaMailProperties.putAll(mailProperties.getCustomProperties());
        }
        
        mailSender.setJavaMailProperties(javaMailProperties);
        
        log.info("JavaMailSender配置完成: host={}, port={}, ssl={}, tls={}",
            smtp.getHost(), smtp.getPort(), smtp.isSsl(), smtp.isTls());
    }
}