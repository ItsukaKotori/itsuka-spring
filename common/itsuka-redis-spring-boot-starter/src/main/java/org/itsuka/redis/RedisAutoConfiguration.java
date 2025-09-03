package org.itsuka.redis;

import lombok.RequiredArgsConstructor;
import org.itsuka.redis.aspect.DbSwitchAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 自动配置类
 *
 * @author Itsuka
 * @since 2023/3/18
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cache", value = "type", havingValue = "redis")
@EnableConfigurationProperties({RedisCacheProperties.class, RedisProperties.class})
@RequiredArgsConstructor
public class RedisAutoConfiguration {
    private final ObjectProvider<RedisConnectionFactory> connectionFactoryObjectProvider;

    /**
     * RedisTemplate Bean
     *
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        CustomRedisTemplate<String, Object> redisTemplate = new CustomRedisTemplate<>();
        redisTemplate.setConnectionFactory(Objects.requireNonNull(connectionFactoryObjectProvider.getIfAvailable()));
        // 设置序列化格式 默认为JDK Serializer
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate() {
        return new CustomStringRedisTemplate(Objects.requireNonNull(connectionFactoryObjectProvider.getIfAvailable()));
    }

    /**
     * 自定义CacheManager
     * 缓存击穿: @Cacheable(sync = true)
     *
     * @param properties RedisCacheProperties
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisCacheProperties properties) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl(properties))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(Objects.requireNonNull(connectionFactoryObjectProvider.getIfAvailable()))
                .cacheDefaults(configuration)
                .build();
    }

    /**
     * Redis Db切换器
     * 基于RedisConnectionFactory
     *
     * @return DbSwitchAspect
     */
    @Bean
    public DbSwitchAspect dbSwitchAspect(RedisProperties redisProperties) {
        return new DbSwitchAspect(Objects.requireNonNull(connectionFactoryObjectProvider.getIfAvailable()), redisProperties);
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
