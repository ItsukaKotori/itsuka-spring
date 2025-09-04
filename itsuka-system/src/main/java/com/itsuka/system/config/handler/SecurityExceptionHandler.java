package com.itsuka.system.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domian.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Spring Security异常处理器
 *
 * @author Itsuka
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AccessDeniedHandler, AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 处理权限不足异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 设置响应状态码
        response.setStatus(HttpStatus.FORBIDDEN.value());
        
        // 设置响应内容类型
        response.setContentType("application/json;charset=UTF-8");
        
        // 构建错误响应
        @SuppressWarnings("unchecked")
        Response<String> errorResponse = (Response<String>) Response.error("权限不足，请联系管理员");
        
        // 写入响应
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(errorResponse));
            writer.flush();
        }
    }
    
    /**
     * 处理未认证异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        // 设置响应状态码
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        
        // 设置响应内容类型
        response.setContentType("application/json;charset=UTF-8");
        
        // 根据异常类型提供不同的错误信息
        String errorMessage = "未授权访问，请先登录";
        if (authException instanceof BadCredentialsException) {
            errorMessage = "用户名或密码错误";
        }
        
        // 构建错误响应
        @SuppressWarnings("unchecked")
        Response<String> errorResponse = (Response<String>) Response.error(errorMessage);
        
        // 写入响应
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(errorResponse));
            writer.flush();
        }
    }
}