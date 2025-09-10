package org.itsuka.dict.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "itsuka.dict", value = "cacheType", havingValue = "LOCAL", matchIfMissing = false)
public class LocalDictCacheConfiguration {
    @Bean
    public DictCacheManager cacheManager() {
        return new LocalCacheManager();
    }
}
