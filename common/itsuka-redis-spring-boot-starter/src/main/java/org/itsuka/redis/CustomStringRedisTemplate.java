package org.itsuka.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

public class CustomStringRedisTemplate extends StringRedisTemplate {
    public CustomStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public RedisConnectionFactory getConnectionFactory() {
        RedisConnectionFactory factory = ConnectionFactoryHolder.get();
        if (factory != null) {
            return factory;
        }
        return super.getConnectionFactory();
    }
}
