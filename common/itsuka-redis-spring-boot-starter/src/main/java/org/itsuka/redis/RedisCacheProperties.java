package org.itsuka.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存属性配置类
 */
@Data
@ConfigurationProperties(prefix = "itsuka.cache")
public class RedisCacheProperties {
    private boolean enable;

    private int ttl;

    /**
     * 用于防止缓存雪崩
     */
    private RandomTtl randomTtl;

    @Data
    public static class RandomTtl {
        private boolean enable;

        private int baseTtl;

        private int floatTtl;
    }
}
