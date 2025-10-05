package org.itsuka.redis;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(prefix = "spring.main", name = "web-application-type", havingValue = "reactive")
public class RedisReactiveConfiguration {
    /**
     * 统一的响应式RedisTemplate配置
     * 强制所有项目使用相同的序列化方式，确保数据一致性
     * 通过EnvironmentPostProcessor自动启用Bean覆盖
     *
     * @param connectionFactory 响应式Redis连接工厂
     * @return ReactiveRedisTemplate
     */
    @Bean("reactiveRedisTemplate")
    @Primary
    @ConditionalOnSingleCandidate(ReactiveRedisConnectionFactory.class)
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(RedisSerializer.string())
                .value(RedisSerializer.json())
                .hashKey(RedisSerializer.string())
                .hashValue(RedisSerializer.json())
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * 统一的响应式字符串RedisTemplate配置
     * 强制所有项目使用相同的序列化方式，确保数据一致性
     * 通过EnvironmentPostProcessor自动启用Bean覆盖
     *
     * @param reactiveRedisConnectionFactory 响应式Redis连接工厂
     * @return ReactiveStringRedisTemplate
     */
    @Bean("reactiveStringRedisTemplate")
    @Primary
    @ConditionalOnBean(ReactiveRedisConnectionFactory.class)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
    }
}
