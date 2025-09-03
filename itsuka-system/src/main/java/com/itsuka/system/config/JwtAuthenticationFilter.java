package com.itsuka.system.config;

import com.itsuka.system.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * @author Itsuka
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取token
        String token = getTokenFromRequest(request);
        
        // 如果token不为空且当前没有已认证的用户
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 从token中提取用户名
                String username = jwtUtils.extractUsername(token);
                
                if (username != null) {
                    // 根据用户名获取用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // 验证token是否有效
                    if (jwtUtils.validateToken(token, userDetails)) {
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        
                        // 设置认证详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置认证信息到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // token验证失败，不影响请求继续处理
                logger.error("无法设置用户认证: {}", e);
            }
        }
        
        // 继续过滤链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中获取token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}