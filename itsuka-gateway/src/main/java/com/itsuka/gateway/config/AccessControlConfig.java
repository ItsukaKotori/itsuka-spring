package com.itsuka.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 访问控制配置类
 *
 * @author itsuka
 */
@Configuration
public class AccessControlConfig {

    /**
     * 路径匹配器Bean
     */
    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }
}