package com.itsuka.system.config.handler;

import org.itsuka.core.exception.AuthenticationException;
import com.itsuka.security.token.TokenProvider;
import com.itsuka.security.toolkit.TokenBlacklistManager;
import com.itsuka.system.dto.authentication.LoginResponse;
import com.itsuka.security.autoconfigure.SecurityProperties;
import org.itsuka.core.domain.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证处理器默认实现，基于Spring Security标准
 *
 * @author Itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationHandler {
    
    private final TokenProvider tokenProvider;

    private final UserDetailsService userDetailsService;

    private final SecurityProperties securityProperties;

    private final TokenBlacklistManager tokenBlacklistManager;

    private final BCryptPasswordEncoder passwordEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginResponse authenticate(String username, String password) {
        try {
            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null || !userDetails.isEnabled()) {
                throw new AuthenticationException("登录失败: 账户名或者密码错误");
            }

            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                throw new AuthenticationException("登录失败: 账户名或者密码错误");
            }
            
            // 生成JWT令牌
            Map<String, Object> claims = new HashMap<>();
            if (userDetails instanceof LoginUser loginUser) {
                claims.put("userId", loginUser.getId().toString());
                claims.put("username", loginUser.getUsername());
                if (loginUser.getAuthorities() != null && !loginUser.getAuthorities().isEmpty()) {
                    claims.put("authorities", loginUser.getAuthorities());
                }
            } else {
                throw new AssertionError("Unexpected UserDetails type: " + userDetails.getClass());
            }
            
            String accessToken = tokenProvider.generateAccessToken(username, claims);
            String refreshToken = tokenProvider.generateRefreshToken(username);
            
            // userDetails 已经是 LoginUser 类型，直接转换并清除密码信息
            LoginUser currentUser = (LoginUser) userDetails;
            LoginUser responseUser = new LoginUser(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getNickname(),
                    null, // 不返回密码
                    currentUser.getEnable(),
                    currentUser.getLock(),
                    currentUser.getAuthorities()
            );
            
            // 将用户信息缓存到Redis，过期时间与JWT保持一致
            cacheUserInfo(currentUser.getId().toString(), responseUser);
            
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(securityProperties.getJwt().getExpiration().toMillis())
                    .username(username)
                    .userId(currentUser.getId().toString())
                    .userInfo(responseUser) // 添加完整用户信息
                    .build();
                    
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            throw new AuthenticationException("Authentication failed", e);
        }
    }
    
    public LoginResponse refreshToken(String refreshToken) {
        try {
            String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // userDetails 已经是 LoginUser 类型，直接转换并清除密码信息
            LoginUser currentUser = (LoginUser) userDetails;
            LoginUser responseUser = new LoginUser(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getNickname(),
                    null, // 不返回密码
                    currentUser.getEnable(),
                    currentUser.getLock(),
                    currentUser.getAuthorities()
            );
            
            // 将用户信息缓存到Redis，过期时间与JWT保持一致
            cacheUserInfo(currentUser.getId().toString(), responseUser);
            
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // 保持原刷新令牌
                    .tokenType("Bearer")
                    .expiresIn(securityProperties.getJwt().getExpiration().toMillis())
                    .username(username)
                    .userId(currentUser.getId().toString())
                    .userInfo(responseUser) // 添加完整用户信息
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AuthenticationException("Failed to refresh token", e);
        }
    }
    
    public UserDetails validateTokenAndGetUserDetails(String token) {
        try {
            if (!tokenProvider.validateToken(token)) {
                return createUnauthenticatedUser();
            }
            
            String username = tokenProvider.getUsernameFromToken(token);

            if (username == null) {
                return createUnauthenticatedUser();
            }
            
            // 从数据库重新加载用户信息以确保最新状态
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null || !userDetails.isEnabled()) {
                return createUnauthenticatedUser();
            }
            
            return userDetails;
                    
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return createUnauthenticatedUser();
        }
    }
    
    /**
     * 创建未认证的用户详情
     */
    private LoginUser createUnauthenticatedUser() {
        return new LoginUser(
                null, null, null, null, false, true, null
        );
    }
    
    /**
     * 缓存用户信息到Redis
     * 
     * @param userId 用户ID
     * @param loginUser 用户信息
     */
    private void cacheUserInfo(String userId, LoginUser loginUser) {
        try {
            String cacheKey = "user:info:" + userId;
            Duration expiration = securityProperties.getJwt().getExpiration();
            redisTemplate.opsForValue().set(cacheKey, loginUser, expiration);
            log.debug("User info cached for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to cache user info for user: {}", userId, e);
        }
    }
    
    /**
     * 从缓存中获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public LoginUser getUserInfoFromCache(String userId) {
        try {
            String cacheKey = "user:info:" + userId;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof LoginUser) {
                return (LoginUser) cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get user info from cache for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 清除用户缓存
     * 
     * @param userId 用户ID
     */
    private void clearUserCache(String userId) {
        try {
            String cacheKey = "user:info:" + userId;
            redisTemplate.delete(cacheKey);
            log.debug("User cache cleared for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear user cache for user: {}", userId, e);
        }
    }
    
    public void logout(String token) {
        try {
            String username = tokenProvider.getUsernameFromToken(token);
            String userId = tokenProvider.getUserIdFromToken(token);
            
            // 获取令牌的过期时间
            io.jsonwebtoken.Claims claims = tokenProvider.getClaimsFromToken(token);
            if (claims != null && claims.getExpiration() != null) {
                // 将令牌添加到黑名单中，防止被继续使用
                tokenBlacklistManager.addToBlacklist(token, claims.getExpiration());
                log.info("User {} logged out, token added to blacklist", username);
            } else {
                log.warn("Failed to get token expiration time for user {}", username);
            }
            
            // 清除Redis中的用户缓存
            if (userId != null) {
                clearUserCache(userId);
                log.info("User cache cleared for user: {}", userId);
            }
            
        } catch (Exception e) {
            log.warn("Failed to process logout for token", e);
        }
    }
}