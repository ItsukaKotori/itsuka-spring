package org.itsuka.dict.interceptor;

import org.itsuka.dict.cache.DictCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 字典拦截器配置类
 * 用于注册字典响应处理器和响应体处理类
 */
@Configuration
@ConditionalOnProperty(prefix = "itsuka.dict", value = "enable", havingValue = "true", matchIfMissing = true)
public class DictInterceptorConfiguration {
    /**
     * 注册字典响应处理器
     * @return 字典响应处理器实例
     */
    @Bean
    public DictResponseHandler dictResponseHandler(DictCacheManager dictCacheManager) {
        return new DictResponseHandler(dictCacheManager);
    }

    /**
     * 注册字典响应体处理
     * @return 字典响应体处理实例
     */
    @Bean
    public DictResponseBodyAdvice dictResponseBodyAdvice(DictResponseHandler dictResponseHandler) {
        return new DictResponseBodyAdvice(dictResponseHandler);
    }
}