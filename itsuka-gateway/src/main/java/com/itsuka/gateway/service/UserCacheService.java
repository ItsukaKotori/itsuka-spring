package com.itsuka.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.LoginUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 用户缓存服务
 * 用于网关中校验和获取Redis中的用户信息
 *
 * @author Itsuka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {
    // 使用统一的Redis序列化配置，由starter自动提供
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    /**
     * 从缓存中获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息的Mono
     */
    public Mono<LoginUser> getUserInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Mono.empty();
        }
        
        String cacheKey = "user:info:" + userId;
        return reactiveRedisTemplate.opsForValue()
                .get(cacheKey)
                .cast(LoginUser.class)
                .doOnNext(user -> log.debug("User info found in cache for user: {}", userId))
                .doOnError(error -> log.warn("Failed to get user info from cache for user: {}", userId, error))
                .onErrorReturn(new LoginUser(null, null, null, null, false, true, null));
    }
    
    /**
     * 检查用户是否在缓存中存在且有效
     * 
     * @param userId 用户ID
     * @return 用户是否有效的Mono
     */
    public Mono<Boolean> isUserValid(String userId) {
        return getUserInfo(userId)
                .map(user -> user.getId() != null && user.isEnabled() && user.isAccountNonLocked())
                .defaultIfEmpty(false);
    }
    
    /**
     * 刷新用户缓存过期时间
     * 
     * @param userId 用户ID
     * @param expiration 过期时间
     * @return 操作结果的Mono
     */
    public Mono<Boolean> refreshUserCacheExpiration(String userId, Duration expiration) {
        if (userId == null || userId.isEmpty()) {
            return Mono.just(false);
        }
        
        String cacheKey = "user:info:" + userId;
        return reactiveRedisTemplate.expire(cacheKey, expiration)
                .doOnNext(result -> {
                    if (result) {
                        log.debug("User cache expiration refreshed for user: {}", userId);
                    } else {
                        log.warn("Failed to refresh cache expiration for user: {}", userId);
                    }
                })
                .onErrorReturn(false);
    }
}