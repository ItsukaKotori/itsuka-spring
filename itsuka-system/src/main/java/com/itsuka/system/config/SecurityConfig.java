package com.itsuka.system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 配置类 - 针对微服务架构优化
 * 认证由网关统一处理，本模块主要提供业务接口和方法级安全
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
            // 禁用CSRF，因为使用JWT令牌认证
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置会话管理：无状态会话，不创建 HttpSession
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 禁用默认认证方式，认证由网关处理
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            
            // 配置安全头
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny()) // 防止点击劫持
                .contentTypeOptions(contentType -> {}) // 防止MIME类型混淆
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1年
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许认证相关端点公开访问
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/logout").permitAll()
                
                // 允许监控和健康检查端点公开访问
                .requestMatchers("/actuator/**").permitAll()
                
                // 允许API文档公开访问
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-resources/**").permitAll()
                
                // 允许静态资源和错误页面
                .requestMatchers("/favicon.ico", "/error").permitAll()
                
                // 其他所有请求需要认证（在网关层处理）
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}