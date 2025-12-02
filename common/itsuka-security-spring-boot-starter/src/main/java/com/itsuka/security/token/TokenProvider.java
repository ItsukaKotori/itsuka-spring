package com.itsuka.security.token;

import io.jsonwebtoken.Claims;
import org.itsuka.core.domain.LoginUser;

import java.util.Map;

/**
 * JWT管理器接口
 *
 * @author Itsuka
 */
public interface TokenProvider {
    
    /**
     * 生成访问令牌
     *
     * @param username 用户名
     * @return JWT令牌
     */
    String generateAccessToken(String username);
    
    /**
     * 生成访问令牌
     *
     * @param username 用户名
     * @param claims 额外的声明
     * @return JWT令牌
     */
    String generateAccessToken(String username, Map<String, Object> claims);
    
    /**
     * 根据LoginUser生成访问令牌
     *
     * @param loginUser 登录用户信息
     * @return JWT访问令牌
     */
    String generateAccessToken(LoginUser loginUser);
    
    /**
     * 根据LoginUser生成刷新令牌
     *
     * @param loginUser 登录用户信息
     * @return JWT刷新令牌
     */
    String generateRefreshToken(LoginUser loginUser);
    
    /**
     * 生成刷新令牌
     *
     * @param username 用户名
     * @return 刷新令牌
     */
    String generateRefreshToken(String username);
    
    /**
     * 验证令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    boolean validateToken(String token);
    
    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    String getUsernameFromToken(String token);
    
    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    String getUserIdFromToken(String token);
    
    /**
     * 从令牌中获取所有声明
     *
     * @param token JWT令牌
     * @return 声明信息
     */
    Claims getClaimsFromToken(String token);
    
    /**
     * 检查令牌是否即将过期
     *
     * @param token JWT令牌
     * @param refreshThresholdMinutes 刷新阈值（分钟）
     * @return 是否即将过期
     */
    boolean isTokenExpiringSoon(String token, long refreshThresholdMinutes);
    
    /**
     * 检查令牌是否在黑名单中
     *
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    String refreshAccessToken(String refreshToken);
}