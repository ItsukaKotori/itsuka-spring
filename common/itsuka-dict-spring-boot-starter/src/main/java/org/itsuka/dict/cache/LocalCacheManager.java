package org.itsuka.dict.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存管理器
 * 使用ConcurrentHashMap保证线程安全
 */
public class LocalCacheManager implements DictCacheManager {
    private static final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public <T> T get(String key, Class<T> tClass) {
        if (key == null || tClass == null) {
            return null;
        }
        Object value = cache.get(key);
        return value != null ? tClass.cast(value) : null;
    }

    @Override
    public <T> void put(String key, T value) {
        if (key != null && value != null) {
            cache.put(key, value);
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
