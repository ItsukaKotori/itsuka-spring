package org.itsuka.dict.cache;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "itsuka.dict", value = "cacheType", havingValue = "REDIS", matchIfMissing = false)
@AutoConfigureAfter({DataRedisAutoConfiguration.class})
public class RedisDictCacheConfiguration {
    @Bean
    public DictCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }
}
