package org.itsuka.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class CustomRedisTemplate<K, V> extends RedisTemplate<K, V> {
    @Override
    public RedisConnectionFactory getConnectionFactory() {
        RedisConnectionFactory factory = ConnectionFactoryHolder.get();
        if (factory != null) {
            return factory;
        }
        return super.getConnectionFactory();
    }
}
