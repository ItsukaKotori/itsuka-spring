package com.itsuka.security.jwt.impl;

import com.itsuka.security.jwt.JwtManager;
import com.itsuka.security.manager.TokenBlacklistManager;
import com.itsuka.security.autoconfigure.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT管理器默认实现
 *
 * @author Itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultJwtManager implements JwtManager {
    
    private final SecurityProperties.JwtProperties jwtProperties;
    private final TokenBlacklistManager tokenBlacklistManager;
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    public String generateAccessToken(String username) {
        return generateAccessToken(username, new HashMap<>());
    }
    
    @Override
    public String generateAccessToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration().toMillis());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    @Override
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshExpiration().toMillis());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            // 先检查令牌是否在黑名单中
            if (tokenBlacklistManager.isBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }
            
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            
            // 检查过期时间
            Date expiration = claims.getExpiration();
            return expiration != null && !expiration.before(new Date());
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            log.debug("Failed to get username from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims != null ? claims.get("userId", String.class) : null;
        } catch (Exception e) {
            log.debug("Failed to get user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Failed to parse token claims: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean isTokenExpiringSoon(String token, long refreshThresholdMinutes) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return true;
            }
            
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return true;
            }
            
            long thresholdMillis = TimeUnit.MINUTES.toMillis(refreshThresholdMinutes);
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            
            return timeUntilExpiration <= thresholdMillis;
        } catch (Exception e) {
            log.debug("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    @Override
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = getClaimsFromToken(refreshToken);
            if (claims == null) {
                throw new IllegalArgumentException("Invalid refresh token");
            }
            
            // 验证是否为刷新令牌
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("Token is not a refresh token");
            }
            
            String username = claims.getSubject();
            if (username == null) {
                throw new IllegalArgumentException("Invalid refresh token: no subject");
            }
            
            // 验证刷新令牌是否过期
            if (!validateToken(refreshToken)) {
                throw new IllegalArgumentException("Refresh token has expired");
            }
            
            // 生成新的访问令牌
            return generateAccessToken(username);
        } catch (Exception e) {
            log.warn("Failed to refresh access token: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to refresh token", e);
        }
    }
    
    @Override
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistManager.isBlacklisted(token);
    }
}