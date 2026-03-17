package org.itsuka.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Import;

/**
 * Redis 自动配置类
 *
 * @author Itsuka
 * @since 2023/3/18
 */
@AutoConfiguration
@EnableConfigurationProperties({RedisCacheProperties.class, DataRedisProperties.class})
@Import({RedisConfiguration.class, RedisReactiveConfiguration.class})
public class RedisAutoConfiguration {

}
