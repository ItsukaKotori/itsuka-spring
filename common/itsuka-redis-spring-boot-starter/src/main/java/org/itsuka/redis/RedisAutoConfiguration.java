package org.itsuka.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 自动配置类
 *
 * @author Itsuka
 * @since 2023/3/18
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cache", value = "type", havingValue = "redis")
@EnableConfigurationProperties(RedisCacheProperties.class)
public class RedisAutoConfiguration {
    /**
     * RedisTemplate Bean
     *
     * @param redisConnectionFactory redis连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置序列化格式 默认为JDK Serializer
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        return redisTemplate;
    }

    /**
     * 自定义CacheManager
     * 缓存击穿: @Cacheable(sync = true)
     *
     * @param factory
     * @param properties
     * @return
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory,
                                     RedisCacheProperties properties) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl(properties))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(configuration)
                .build();
    }


    private Duration ttl(RedisCacheProperties properties) {
        RedisCacheProperties.RandomTtl randomTtl = properties.getRandomTtl();
        if (!randomTtl.isEnable()) {
            return Duration.ofSeconds(properties.getTtl());
        }
        int baseTtl = randomTtl.getBaseTtl();
        int floatTtl = randomTtl.getFloatTtl();
        int random = ThreadLocalRandom.current().nextInt(-floatTtl, floatTtl) + baseTtl;
        return Duration.ofSeconds(random);
    }
}
