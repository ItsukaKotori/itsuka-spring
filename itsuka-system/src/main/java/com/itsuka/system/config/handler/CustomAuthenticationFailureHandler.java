package com.itsuka.system.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.itsuka.core.domian.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义认证失败处理器
 *
 * @author Itsuka
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        // 设置响应头
        response.setContentType("application/json;charset=UTF-8");
        
        // 构建失败响应
        @SuppressWarnings("unchecked")
        Response<String> errorResponse = (Response<String>) Response.error("登录失败：" + exception.getMessage());
        
        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}