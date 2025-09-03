package com.itsuka.system.controller;

import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domian.Response;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Itsuka
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SysUserService sysUserService;
    
    /**
     * 未授权访问处理
     */
    @GetMapping("/unauthorized")
    public Response<String> unauthorized() {
        @SuppressWarnings("unchecked")
        Response<String> errorResponse = (Response<String>) Response.error("未授权访问，请先登录");
        return errorResponse;
    }
    
    /**
     * 注册新用户
     */
    @PostMapping("/register")
    public Response<String> register(@RequestBody UserCreateDto createDto) {
        // 注册用户
        Boolean result = sysUserService.create(createDto);
        if (result) {
            @SuppressWarnings("unchecked")
            Response<String> successResponse = (Response<String>) Response.success("注册成功");
            return successResponse;
        } else {
            @SuppressWarnings("unchecked")
            Response<String> errorResponse = (Response<String>) Response.error("注册失败");
            return errorResponse;
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public Response<Authentication> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        @SuppressWarnings("unchecked")
        Response<Authentication> successResponse = (Response<Authentication>) Response.success(authentication);
        return successResponse;
    }
}