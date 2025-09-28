package com.itsuka.system.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsuka.system.utils.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.Response;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义认证成功处理器
 *
 * @author Itsuka
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private final JwtUtils jwtUtils;
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        // 设置响应头
        response.setContentType("application/json;charset=UTF-8");
        
        // 获取用户名
        String username = authentication.getName();
        
        // 生成JWT令牌
        String token = jwtUtils.generateToken(username);
        
        // 构建响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        
        // 构建成功响应
        Response<Map<String, Object>> successResponse = Response.success(data);
        
        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(successResponse));
    }
}