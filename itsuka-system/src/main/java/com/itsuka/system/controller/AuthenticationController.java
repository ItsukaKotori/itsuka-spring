package com.itsuka.system.controller;

import com.itsuka.system.config.handler.AuthenticationHandler;
import com.itsuka.system.dto.authentication.LoginRequest;
import com.itsuka.system.dto.authentication.LoginResponse;
import com.itsuka.system.dto.authentication.RefreshTokenRequest;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.Response;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - 简化版本，认证由网关统一处理
 *
 * @author Itsuka
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final UserService userService;

    private final AuthenticationHandler authenticationHandler;
    
    /**
     * 注册新用户
     */
    @PostMapping("/register")
    public Response<?> register(@RequestBody UserCreateDto createDto) {
        // 注册用户
        Boolean result = userService.create(createDto);
        if (result) {
            return Response.success("注册成功");
        } else {
            return Response.error("注册失败");
        }
    }

    /**
     * 用户登录
     * 返回登录响应，包含令牌信息和完整的用户详情（LoginUser）
     */
    @PostMapping("/login")
    public Response<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse authResponse = authenticationHandler.authenticate(
                request.getUsername(),
                request.getPassword()
        );
        log.info("User {} logged in successfully", request.getUsername());
        return Response.success(authResponse);
    }

    /**
     * 刷新令牌
     * 返回新的访问令牌和完整的用户详情（LoginUser）
     */
    @PostMapping("/refresh")
    public Response<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse authResponse = authenticationHandler.refreshToken(request.getRefreshToken());
        log.info("Token refreshed successfully");
        return Response.success(authResponse);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Response<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationHandler.logout(token);
        }
        log.info("User logged out successfully");
        return Response.success("注销成功");
    }

    
    /**
     * 获取当前登录用户信息 - 仅用于系统内部调用
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public Response<Authentication> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Response.success(authentication);
    }
}