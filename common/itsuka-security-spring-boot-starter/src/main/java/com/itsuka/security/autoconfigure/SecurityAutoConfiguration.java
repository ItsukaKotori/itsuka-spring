package com.itsuka.security.autoconfigure;

import com.itsuka.security.manager.AccessControlManager;
import com.itsuka.security.token.JwtTokenProvider;
import com.itsuka.security.token.TokenProvider;
import com.itsuka.security.toolkit.TokenBlacklistManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.AntPathMatcher;

/**
 * 安全框架自动配置类
 *
 * @author Itsuka
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({ TokenProvider.class })
@ConditionalOnProperty(prefix = "itsuka.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ SecurityProperties.class, AccessControlProperties.class })
@AutoConfigureAfter(RedisAutoConfiguration.class)
@RequiredArgsConstructor
public class SecurityAutoConfiguration {
    private final SecurityProperties securityProperties;

    /**
     * JWT管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "itsuka.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TokenProvider tokenProvider(TokenBlacklistManager tokenBlacklistManager) {
        log.info("Initializing JWT manager with issuer: {}", securityProperties.getJwt().getIssuer());
        return new JwtTokenProvider(securityProperties.getJwt(), tokenBlacklistManager);
    }

    /**
     * 令牌黑名单管理器Bean - Servlet环境
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "spring.main", name = "web-application-type", havingValue = "servlet", matchIfMissing = true)
    public TokenBlacklistManager tokenBlacklistManager(RedisTemplate<String, Object> redisTemplate) {
        log.info("Initializing token blacklist manager for servlet environment");
        return new TokenBlacklistManager(redisTemplate, securityProperties.getJwt());
    }

    /**
     * 令牌黑名单管理器Bean - Reactive环境
     */
    @Bean
    @ConditionalOnBean(ReactiveRedisTemplate.class)
    @ConditionalOnProperty(prefix = "spring.main", name = "web-application-type", havingValue = "reactive")
    public TokenBlacklistManager reactiveTokenBlacklistManager(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        log.info("Initializing token blacklist manager for reactive environment");
        return new TokenBlacklistManager(reactiveRedisTemplate, securityProperties.getJwt());
    }

    /**
     * 统一的密码编码器Bean
     * 强制所有项目使用BCrypt密码编码，确保安全性一致
     * 直接覆盖其他可能的PasswordEncoder实现
     */
    @Bean("passwordEncoder")
    @Primary
    public BCryptPasswordEncoder passwordEncoder() {
        log.info("Initializing BCrypt password encoder");
        return new BCryptPasswordEncoder();
    }

    /**
     * 路径匹配器Bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public AntPathMatcher antPathMatcher() {
        log.info("Initializing AntPathMatcher for access control");
        return new AntPathMatcher();
    }

    /**
     * 访问控制管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "itsuka.security.access-control", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessControlManager accessControlManager(AccessControlProperties accessControlProperties,
            AntPathMatcher antPathMatcher) {
        log.info("Initializing AccessControlManager with enabled: {}", accessControlProperties.isEnabled());
        return new AccessControlManager(accessControlProperties, antPathMatcher);
    }
}