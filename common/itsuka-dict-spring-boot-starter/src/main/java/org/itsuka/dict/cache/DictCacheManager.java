package org.itsuka.dict.cache;

import lombok.val;

public interface DictCacheManager {
    /**
     * 根据Key获取缓存值
     * 
     * @param <T>
     * @param key
     * @param tClass
     * @return
     */
    <T> T get(String key, Class<T> tClass);

    /**
     * 设置缓存值
     * 
     * @param <T>
     * @param key
     * @param value
     */
    <T> void put(String key, T value);

    /**
     * 清空缓存
     */
    void clear();
}
