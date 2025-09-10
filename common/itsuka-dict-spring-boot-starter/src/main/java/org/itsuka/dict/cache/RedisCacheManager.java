package org.itsuka.dict.cache;

import org.springframework.data.redis.core.RedisTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisCacheManager implements DictCacheManager {
    private static final String DICT_CACHE_PREFIX = "dict:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T get(String key, Class<T> tClass) {
        Object value = redisTemplate.opsForValue().get(DICT_CACHE_PREFIX + key);
        return value != null ? tClass.cast(value) : null;
    }

    @Override
    public <T> void put(String key, T value) {
        redisTemplate.opsForValue().set(DICT_CACHE_PREFIX + key, value);
    }

    @Override
    public void clear() {
        redisTemplate.delete(DICT_CACHE_PREFIX + "*");
    }

}
