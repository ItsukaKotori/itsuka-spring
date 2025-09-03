package org.itsuka.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;

public class ConnectionFactoryHolder {
    private static final ThreadLocal<RedisConnectionFactory> THREAD_LOCAL = new ThreadLocal<>();

    public static RedisConnectionFactory get() {
        return THREAD_LOCAL.get();
    }

    public static void set(RedisConnectionFactory connectionFactory) {
        THREAD_LOCAL.set(connectionFactory);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}