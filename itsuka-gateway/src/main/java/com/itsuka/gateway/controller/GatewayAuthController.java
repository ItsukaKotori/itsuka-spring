package com.itsuka.gateway.controller;

import com.itsuka.security.model.AuthResponse;
import com.itsuka.security.handler.AuthenticationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.Response;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 网关认证控制器
 * 统一处理所有认证相关请求
 *
 * @author Itsuka
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GatewayAuthController {
    
    private final AuthenticationHandler authenticationHandler;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Mono<Response<AuthResponse>> login(@RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> {
            try {
                AuthResponse authResponse = authenticationHandler.authenticate(
                        request.getUsername(), 
                        request.getPassword()
                );
                log.info("User {} logged in successfully", request.getUsername());
                return (Response<AuthResponse>) Response.success(authResponse);
            } catch (Exception e) {
                log.warn("Login failed for user: {}, error: {}", request.getUsername(), e.getMessage());
                return (Response<AuthResponse>) Response.error("登录失败：" + e.getMessage());
            }
        });
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Mono<Response<AuthResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        return Mono.fromCallable(() -> {
            try {
                AuthResponse authResponse = authenticationHandler.refreshToken(request.getRefreshToken());
                log.info("Token refreshed successfully");
                return (Response<AuthResponse>) Response.success(authResponse);
            } catch (Exception e) {
                log.warn("Token refresh failed: {}", e.getMessage());
                return (Response<AuthResponse>) Response.error("令牌刷新失败：" + e.getMessage());
            }
        });
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Mono<Response<String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return Mono.fromCallable(() -> {
            try {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    authenticationHandler.logout(token);
                }
                log.info("User logged out successfully");
                return (Response<String>) Response.success("注销成功");
            } catch (Exception e) {
                log.warn("Logout failed: {}", e.getMessage());
                return (Response<String>) Response.error("注销失败：" + e.getMessage());
            }
        });
    }
    
    /**
     * 未授权访问处理
     */
    @GetMapping("/unauthorized")
    public Mono<Response<String>> unauthorized() {
        return Mono.just((Response<String>) Response.error("未授权访问，请先登录"));
    }
    
    /**
     * 权限不足处理
     */
    @GetMapping("/access-denied")
    public Mono<Response<String>> accessDenied() {
        return Mono.just((Response<String>) Response.error("权限不足，拒绝访问"));
    }
    
    /**
     * 登录请求模型
     */
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    /**
     * 刷新令牌请求模型
     */
    public static class RefreshTokenRequest {
        private String refreshToken;
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}