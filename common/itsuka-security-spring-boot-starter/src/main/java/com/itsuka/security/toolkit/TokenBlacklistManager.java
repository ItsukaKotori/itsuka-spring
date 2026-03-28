package com.itsuka.security.toolkit;

import com.itsuka.security.autoconfigure.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
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
    private static final String PREFIX = "blacklist:";

    private final RedisOperationsDelegate redisDelegate;
    private final SecurityProperties.JwtProperties properties;

    /**
     * Constructor for blocking RedisTemplate (Servlet environment)
     */
    public TokenBlacklistManager(RedisTemplate<String, Object> redisTemplate, SecurityProperties.JwtProperties properties) {
        this.properties = properties;
        this.redisDelegate = new BlockingRedisDelegate(redisTemplate);
    }

    /**
     * Constructor for ReactiveRedisTemplate (Reactive environment)
     */
    public TokenBlacklistManager(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate, SecurityProperties.JwtProperties properties) {
        this.properties = properties;
        this.redisDelegate = new ReactiveRedisDelegate(reactiveRedisTemplate);
    }

    /**
     * 将令牌添加到黑名单
     *
     * @param token 令牌
     */
    public void addToBlacklist(String token) {
        if (token != null) {
            redisDelegate.set(PREFIX + token, 1, properties.getExpiration());
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
        return redisDelegate.hasKey(PREFIX + token);
    }

    /**
     * 从黑名单中移除令牌
     *
     * @param token 令牌
     */
    public void removeFromBlacklist(String token) {
        if (token != null) {
            redisDelegate.delete(PREFIX + token);
            log.debug("Token removed from blacklist");
        }
    }

    /**
     * 清空黑名单
     */
    public void clearBlacklist() {
        ScanOptions scanOptions = ScanOptions.scanOptions().match(PREFIX + "*").count(100).build();
        Set<String> keys = redisDelegate.scanKeys(scanOptions);

        if (!keys.isEmpty()) {
            long count = redisDelegate.deleteBatch(keys);
            log.info("Token blacklist cleared, {} keys removed", count);
        } else {
            log.info("Token blacklist is empty");
        }
    }

    /**
     * Redis操作代理接口
     * 统一封装阻塞和响应式两种Redis操作方式
     */
    private interface RedisOperationsDelegate {
        void set(String key, Object value, Duration expiration);

        boolean hasKey(String key);

        void delete(String key);

        Set<String> scanKeys(ScanOptions options);

        long deleteBatch(Set<String> keys);
    }

    /**
     * 阻塞式Redis操作实现
     */
    @Slf4j
    @RequiredArgsConstructor
    private static class BlockingRedisDelegate implements RedisOperationsDelegate {
        private final RedisTemplate<String, Object> redisTemplate;

        @Override
        public void set(String key, Object value, Duration expiration) {
            redisTemplate.opsForValue().set(key, value, expiration.getSeconds(), TimeUnit.SECONDS);
        }

        @Override
        public boolean hasKey(String key) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }

        @Override
        public void delete(String key) {
            redisTemplate.delete(key);
        }

        @Override
        public Set<String> scanKeys(ScanOptions options) {
            Set<String> keys = new HashSet<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            return keys;
        }

        @Override
        public long deleteBatch(Set<String> keys) {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        }
    }

    /**
     * 响应式Redis操作实现（阻塞封装）
     */
    @Slf4j
    @RequiredArgsConstructor
    private static class ReactiveRedisDelegate implements RedisOperationsDelegate {
        private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

        @Override
        public void set(String key, Object value, Duration expiration) {
            reactiveRedisTemplate.opsForValue().set(key, value, expiration).block();
        }

        @Override
        public boolean hasKey(String key) {
            Boolean result = reactiveRedisTemplate.hasKey(key).block();
            return Boolean.TRUE.equals(result);
        }

        @Override
        public void delete(String key) {
            reactiveRedisTemplate.delete(key).block();
        }

        @Override
        public Set<String> scanKeys(ScanOptions options) {
            List<String> keys = reactiveRedisTemplate.scan(options)
                    .collectList()
                    .block();
            return keys != null ? new HashSet<>(keys) : new HashSet<>();
        }

        @Override
        public long deleteBatch(Set<String> keys) {
            Long count = reactiveRedisTemplate.delete(keys.toArray(new String[0])).block();
            return count != null ? count : 0;
        }
    }
}
