package com.itsuka.security.handler.impl;

import com.itsuka.security.contract.UserDetailsProvider;
import com.itsuka.security.exception.AuthenticationException;
import com.itsuka.security.handler.AuthenticationHandler;
import com.itsuka.security.jwt.JwtManager;
import com.itsuka.security.manager.TokenBlacklistManager;
import com.itsuka.security.model.AuthResponse;
import com.itsuka.security.model.SecurityUserDetails;
import com.itsuka.security.autoconfigure.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证处理器默认实现，基于Spring Security标准
 *
 * @author Itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAuthenticationHandler implements AuthenticationHandler {
    
    private final JwtManager jwtManager;
    private final UserDetailsProvider userDetailsProvider;
    private final SecurityProperties.JwtProperties jwtProperties;
    private final TokenBlacklistManager tokenBlacklistManager;
    
    @Override
    public AuthResponse authenticate(String username, String password) {
        try {
            // 验证用户凭据
            if (!userDetailsProvider.validatePassword(username, password)) {
                throw new AuthenticationException("Invalid username or password");
            }
            
            // 加载用户详情
            UserDetails userDetails = userDetailsProvider.loadUserByUsername(username);
            if (userDetails == null || !userDetails.isEnabled()) {
                throw new AuthenticationException("User not found or disabled");
            }
            
            // 生成JWT令牌
            Map<String, Object> claims = new HashMap<>();
            if (userDetails instanceof SecurityUserDetails) {
                SecurityUserDetails securityUser = (SecurityUserDetails) userDetails;
                claims.put("userId", securityUser.getUserId());
                if (securityUser.getAuthorities() != null && !securityUser.getAuthorities().isEmpty()) {
                    claims.put("authorities", securityUser.getAuthorities());
                }
            } else {
                // 处理标准UserDetails
                if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
                    claims.put("authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet()));
                }
            }
            
            String accessToken = jwtManager.generateAccessToken(username, claims);
            String refreshToken = jwtManager.generateRefreshToken(username);
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration().toMillis())
                    .username(username)
                    .userId(userDetails instanceof SecurityUserDetails ? 
                            ((SecurityUserDetails) userDetails).getUserId() : null)
                    .build();
                    
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            throw new AuthenticationException("Authentication failed", e);
        }
    }
    
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String newAccessToken = jwtManager.refreshAccessToken(refreshToken);
            String username = jwtManager.getUsernameFromToken(refreshToken);
            
            UserDetails userDetails = userDetailsProvider.loadUserByUsername(username);
            
            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // 保持原刷新令牌
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration().toMillis())
                    .username(username)
                    .userId(userDetails instanceof SecurityUserDetails ? 
                            ((SecurityUserDetails) userDetails).getUserId() : null)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AuthenticationException("Failed to refresh token", e);
        }
    }
    
    @Override
    public SecurityUserDetails validateTokenAndGetUserDetails(String token) {
        try {
            if (!jwtManager.validateToken(token)) {
                return SecurityUserDetails.unauthenticated();
            }
            
            String username = jwtManager.getUsernameFromToken(token);
            String userId = jwtManager.getUserIdFromToken(token);
            
            if (username == null) {
                return SecurityUserDetails.unauthenticated();
            }
            
            // 从数据库重新加载用户信息以确保最新状态
            UserDetails userDetails = userDetailsProvider.loadUserByUsername(username);
            if (userDetails == null || !userDetails.isEnabled()) {
                return SecurityUserDetails.unauthenticated();
            }
            
            // 构建包含令牌的用户详情
            if (userDetails instanceof SecurityUserDetails) {
                SecurityUserDetails securityUser = (SecurityUserDetails) userDetails;
                return securityUser.toBuilder()
                        .token(token)
                        .userId(userId != null ? userId : securityUser.getUserId())
                        .build();
            } else {
                // 转换标准UserDetails为SecurityUserDetails
                return SecurityUserDetails.builder()
                        .userId(userId)
                        .username(username)
                        .enabled(userDetails.isEnabled())
                        .accountNonExpired(userDetails.isAccountNonExpired())
                        .accountNonLocked(userDetails.isAccountNonLocked())
                        .credentialsNonExpired(userDetails.isCredentialsNonExpired())
                        .authorities(userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet()))
                        .token(token)
                        .build();
            }
                    
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return SecurityUserDetails.unauthenticated();
        }
    }
    
    @Override
    public void logout(String token) {
        try {
            String username = jwtManager.getUsernameFromToken(token);
            
            // 获取令牌的过期时间
            io.jsonwebtoken.Claims claims = jwtManager.getClaimsFromToken(token);
            if (claims != null && claims.getExpiration() != null) {
                // 将令牌添加到黑名单中，防止被继续使用
                tokenBlacklistManager.addToBlacklist(token, claims.getExpiration());
                log.info("User {} logged out, token added to blacklist", username);
            } else {
                log.warn("Failed to get token expiration time for user {}", username);
            }
            
        } catch (Exception e) {
            log.warn("Failed to process logout for token", e);
        }
    }
}