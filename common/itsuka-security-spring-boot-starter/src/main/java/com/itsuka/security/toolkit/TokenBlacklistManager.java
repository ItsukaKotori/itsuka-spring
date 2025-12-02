package com.itsuka.security.toolkit;

import com.itsuka.security.autoconfigure.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 令牌黑名单管理器
 * 用于管理已注销或失效的JWT令牌
 *
 * @author Itsuka
 */
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistManager {
    private final static String PREFIX = "blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    private final SecurityProperties.JwtProperties properties;

    /**
     * 将令牌添加到黑名单
     *
     * @param token 令牌
     */
    public void addToBlacklist(String token) {
        if (token != null) {
            Duration expiration = properties.getExpiration();
            redisTemplate.opsForValue().set(PREFIX + token, 1, expiration.getSeconds(), TimeUnit.SECONDS);
            log.debug("Token added to blacklist");
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

        return redisTemplate.hasKey(PREFIX + token);
    }

    /**
     * 从黑名单中移除令牌
     *
     * @param token 令牌
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            redisTemplate.delete(PREFIX + token);
            log.debug("Token removed from blacklist");
        }
    }

    /**
     * 清空黑名单
     */
    public void clearBlacklist() {
        ScanOptions scanOptions = ScanOptions.scanOptions().match(PREFIX + "*").count(100).build();
        Set<String> keys = new HashSet<>();
        
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Token blacklist cleared, {} keys removed", keys.size());
        } else {
            log.info("Token blacklist is empty");
        }
    }
}