package com.itsuka.security.autoconfigure;

import com.itsuka.security.contract.UserDetailsProvider;
import com.itsuka.security.handler.AuthenticationHandler;
import com.itsuka.security.handler.impl.DefaultAuthenticationHandler;
import com.itsuka.security.jwt.JwtManager;
import com.itsuka.security.jwt.impl.DefaultJwtManager;
import com.itsuka.security.manager.TokenBlacklistManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 安全框架自动配置类
 *
 * @author Itsuka
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({JwtManager.class})
@ConditionalOnProperty(prefix = "itsuka.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({SecurityProperties.class})
public class SecurityAutoConfiguration {
    
    /**
     * JWT管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "itsuka.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JwtManager jwtManager(SecurityProperties securityProperties, TokenBlacklistManager tokenBlacklistManager) {
        log.info("Initializing JWT manager with issuer: {}", securityProperties.getJwt().getIssuer());
        return new DefaultJwtManager(securityProperties.getJwt(), tokenBlacklistManager);
    }
    
    /**
     * 认证处理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({JwtManager.class, UserDetailsProvider.class})
    public AuthenticationHandler authenticationHandler(
            JwtManager jwtManager,
            ObjectProvider<UserDetailsProvider> userDetailsProvider,
            SecurityProperties securityProperties,
            TokenBlacklistManager tokenBlacklistManager) {
        log.info("Initializing authentication handler");
        return new DefaultAuthenticationHandler(jwtManager, userDetailsProvider.getIfAvailable(), securityProperties.getJwt(), tokenBlacklistManager);
    }
    
    /**
     * 令牌黑名单管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public TokenBlacklistManager tokenBlacklistManager() {
        log.info("Initializing token blacklist manager");
        return new TokenBlacklistManager();
    }
}