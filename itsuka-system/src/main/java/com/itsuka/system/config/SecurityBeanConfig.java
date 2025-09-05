package com.itsuka.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关的Bean配置类
 * 单独抽离出来以避免循环依赖
 */
@Configuration
public class SecurityBeanConfig {
    
    /**
     * 密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}