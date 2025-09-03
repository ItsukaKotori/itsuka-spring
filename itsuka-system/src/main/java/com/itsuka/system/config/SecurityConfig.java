package com.itsuka.system.config;

import com.itsuka.system.config.handler.CustomAuthenticationFailureHandler;
import com.itsuka.system.config.handler.CustomAuthenticationSuccessHandler;
import com.itsuka.system.config.handler.CustomLogoutSuccessHandler;
import com.itsuka.system.config.handler.SecurityExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 *
 * @author Itsuka
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    
    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;
    
    @Autowired
    private CustomLogoutSuccessHandler logoutSuccessHandler;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private SecurityExceptionHandler securityExceptionHandler;
    
    /**
     * 密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 设置用户详情服务
            .userDetailsService(userDetailsService)
            // 关闭CSRF保护
            .csrf(AbstractHttpConfigurer::disable)
            // 配置URL访问权限
            .authorizeHttpRequests(authorize -> authorize
                // 允许Swagger相关路径匿名访问
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 允许认证相关接口匿名访问
                .requestMatchers("/api/auth/**").permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            // 配置表单登录
            .formLogin(form -> form
                // 自定义登录页面
                .loginPage("/api/auth/unauthorized")
                // 自定义登录处理URL
                .loginProcessingUrl("/api/auth/login")
                // 登录成功处理器
                .successHandler(authenticationSuccessHandler)
                // 登录失败处理器
                .failureHandler(authenticationFailureHandler)
            )
            // 配置注销
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // 配置未授权访问处理器
            .exceptionHandling(exception -> exception
                // 处理未认证异常
                .authenticationEntryPoint(securityExceptionHandler)
                // 处理权限不足异常
                .accessDeniedHandler(securityExceptionHandler)
            )
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}