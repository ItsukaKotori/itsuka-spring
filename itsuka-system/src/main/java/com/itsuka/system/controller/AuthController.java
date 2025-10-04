package com.itsuka.system.controller;

import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.service.UserService;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
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
     * 获取当前登录用户信息 - 仅用于系统内部调用
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public Response<Authentication> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Response.success(authentication);
    }
}