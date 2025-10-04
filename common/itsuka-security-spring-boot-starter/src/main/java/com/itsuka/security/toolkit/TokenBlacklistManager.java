package com.itsuka.security.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 令牌黑名单管理器
 * 用于管理已注销或失效的JWT令牌
 *
 * @author Itsuka
 */
@Slf4j
@Component
public class TokenBlacklistManager {
    
    /**
     * 黑名单缓存 - key: token, value: 过期时间
     */
    private final ConcurrentMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public TokenBlacklistManager() {
        // 每5分钟清理一次过期的黑名单令牌
        /*
          定时清理器
         */
        ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-blacklist-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupExpiredTokens, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 将令牌添加到黑名单
     *
     * @param token 令牌
     * @param expirationTime 令牌原始过期时间
     */
    public void addToBlacklist(String token, Date expirationTime) {
        if (token != null && expirationTime != null) {
            blacklistedTokens.put(token, expirationTime.getTime());
            log.debug("Token added to blacklist, expires at: {}", expirationTime);
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     *
     * @param token 令牌
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        
        Long expirationTime = blacklistedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }
        
        // 如果令牌已过期，从黑名单中移除
        if (expirationTime < System.currentTimeMillis()) {
            blacklistedTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * 从黑名单中移除令牌
     *
     * @param token 令牌
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            blacklistedTokens.remove(token);
            log.debug("Token removed from blacklist");
        }
    }
    
    /**
     * 清理过期的黑名单令牌
     */
    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        
        // 使用AtomicInteger来避免 lambda 中的变量访问问题
        java.util.concurrent.atomic.AtomicInteger removedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        blacklistedTokens.entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                removedCount.incrementAndGet();
                return true;
            }
            return false;
        });
        
        if (removedCount.get() > 0) {
            log.debug("Cleaned up {} expired tokens from blacklist", removedCount.get());
        }
    }
    
    /**
     * 获取黑名单大小
     *
     * @return 黑名单中的令牌数量
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
    
    /**
     * 清空黑名单
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
        log.info("Token blacklist cleared");
    }
}