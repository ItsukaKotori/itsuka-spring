package com.itsuka.security.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 安全框架配置属性
 *
 * @author Itsuka
 */
@Data
@ConfigurationProperties(prefix = "itsuka.security")
public class SecurityProperties {
    
    /**
     * 是否启用安全框架
     */
    private boolean enabled = true;
    
    /**
     * JWT配置
     */
    @NestedConfigurationProperty
    private JwtProperties jwt = new JwtProperties();
    
    /**
     * 认证配置
     */
    @NestedConfigurationProperty
    private AuthProperties auth = new AuthProperties();
    
    /**
     * JWT配置属性
     */
    @Data
    public static class JwtProperties {
        
        /**
         * JWT签名密钥
         */
        private String secret = "itsuka-default-secret-key-for-jwt-token-generation-very-long-key";
        
        /**
         * JWT过期时间
         */
        private Duration expiration = Duration.ofHours(24);
        
        /**
         * JWT刷新令牌过期时间
         */
        private Duration refreshExpiration = Duration.ofDays(7);
        
        /**
         * JWT issuer
         */
        private String issuer = "itsuka-security";
        
        /**
         * JWT audience
         */
        private String audience = "itsuka-app";
        
        /**
         * 是否启用JWT
         */
        private boolean enabled = true;

        /**
         * Token前缀
         */
        private String prefix = "Bearer ";
    }
    
    /**
     * 认证配置属性
     */
    @Data
    public static class AuthProperties {
        
        /**
         * 是否启用认证
         */
        private boolean enabled = true;
        
        /**
         * 不需要认证的路径列表
         */
        private List<String> excludedPaths = Arrays.asList(
                "/api/auth/login",
                "/api/auth/register", 
                "/api/auth/refresh",
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/error"
        );
        
        /**
         * 登录URL
         */
        private String loginUrl = "/api/auth/login";
        
        /**
         * 登录成功跳转URL
         */
        private String loginSuccessUrl = "/";
        
        /**
         * 登录失败跳转URL
         */
        private String loginFailureUrl = "/api/auth/login?error";
        
        /**
         * 注销URL
         */
        private String logoutUrl = "/api/auth/logout";
        
        /**
         * 注销成功跳转URL
         */
        private String logoutSuccessUrl = "/api/auth/login?logout";
        
        /**
         * 未授权访问跳转URL
         */
        private String unauthorizedUrl = "/api/auth/unauthorized";
        
        /**
         * 权限不足跳转URL
         */
        private String accessDeniedUrl = "/api/auth/access-denied";
    }
}