package org.itsuka.redis;

import lombok.RequiredArgsConstructor;
import org.itsuka.redis.aspect.DbSwitchAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@ConditionalOnProperty(prefix = "spring.main", name = "web-application-type",
        havingValue = "servlet", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisConfiguration {
    private final ObjectProvider<RedisConnectionFactory> connectionFactoryObjectProvider;

    /**
     * 自定义CacheManager
     * 缓存击穿: @Cacheable(sync = true)
     *
     * @param properties RedisCacheProperties
     * @return CacheManager
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", value = "type", havingValue = "redis")
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

    /**
     * 统一的RedisTemplate Bean配置
     * 强制所有项目使用相同的序列化方式，确保数据一致性
     * 通过EnvironmentPostProcessor自动启用Bean覆盖
     *
     * @return RedisTemplate
     */
    @Bean("redisTemplate")
    @Primary
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

    /**
     * 统一的StringRedisTemplate Bean配置
     * 强制所有项目使用相同的序列化方式，确保数据一致性
     * 通过EnvironmentPostProcessor自动启用Bean覆盖
     *
     * @return StringRedisTemplate
     */
    @Bean("stringRedisTemplate")
    @Primary
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate() {
        return new CustomStringRedisTemplate(Objects.requireNonNull(connectionFactoryObjectProvider.getIfAvailable()));
    }

    private Duration ttl(RedisCacheProperties properties) {
        RedisCacheProperties.RandomTtl randomTtl = properties.getRandomTtl();
        if (randomTtl == null || !randomTtl.isEnable()) {
            return Duration.ofSeconds(properties.getTtl());
        }
        int baseTtl = randomTtl.getBaseTtl();
        int floatTtl = randomTtl.getFloatTtl();
        int random = ThreadLocalRandom.current().nextInt(-floatTtl, floatTtl) + baseTtl;
        return Duration.ofSeconds(random);
    }
}
