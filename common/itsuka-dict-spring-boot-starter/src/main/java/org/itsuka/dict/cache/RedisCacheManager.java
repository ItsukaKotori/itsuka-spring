package org.itsuka.dict.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

/**
 * Redis缓存管理器
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheManager implements DictCacheManager {
    private static final String DICT_CACHE_PREFIX = "dict:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T get(String key, Class<T> tClass) {
        if (key == null || tClass == null) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(DICT_CACHE_PREFIX + key);
            return value != null ? tClass.cast(value) : null;
        } catch (Exception e) {
            log.error("从 Redis 获取字典缓存失败, key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> void put(String key, T value) {
        if (key == null || value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(DICT_CACHE_PREFIX + key, value);
        } catch (Exception e) {
            log.error("向 Redis 设置字典缓存失败, key: {}", key, e);
        }
    }

    @Override
    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys(DICT_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除了 {} 个字典缓存项", keys.size());
            }
        } catch (Exception e) {
            log.error("清除 Redis 字典缓存失败", e);
        }
    }
}
