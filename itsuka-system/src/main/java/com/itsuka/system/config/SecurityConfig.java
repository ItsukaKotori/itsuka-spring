package com.itsuka.system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类 - 简化版本，主要用于方法级安全
 * 认证由网关统一处理，本模块不再处理登录登出逻辑
 *
 * @author Itsuka
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用表单登录，认证由网关处理
            .formLogin(AbstractHttpConfigurer::disable)
            // 禁用HTTP Basic认证
            .httpBasic(AbstractHttpConfigurer::disable)
            // 禁用默认登出
            .logout(AbstractHttpConfigurer::disable)
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许注册端点公开访问
                .requestMatchers("/api/auth/register").permitAll()
                // 允许Actuator端点公开访问
                .requestMatchers("/actuator/**").permitAll()
                // 允许Swagger文档公开访问
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 其他所有请求需要认证（在网关层处理）
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}