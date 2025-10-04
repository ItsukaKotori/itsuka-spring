package com.itsuka.security.handler;

import com.itsuka.security.model.AuthResponse;
import com.itsuka.security.model.SecurityUserDetails;

/**
 * 认证处理器接口
 *
 * @author Itsuka
 */
public interface AuthenticationHandler {
    
    /**
     * 用户登录认证
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证结果
     */
    AuthResponse authenticate(String username, String password);
    
    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证结果
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * 验证令牌并获取用户上下文
     *
     * @param token JWT令牌
     * @return 用户详情
     */
    SecurityUserDetails validateTokenAndGetUserDetails(String token);
    
    /**
     * 用户登出
     *
     * @param token JWT令牌
     */
    void logout(String token);
}