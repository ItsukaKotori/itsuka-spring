package org.itsuka.dict.cache;

import java.util.HashMap;

public class LocalCacheManager implements DictCacheManager {
    private static final HashMap<String, Object> cache = new HashMap<>();

    @Override
    public <T> T get(String key, Class<T> tClass) {
        Object value = cache.get(key);
        return value != null ? tClass.cast(value) : null;
    }

    @Override
    public <T> void put(String key, T value) {
       cache.put(key, value);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
