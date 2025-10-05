
package org.itsuka.dict;

import org.itsuka.dict.cache.DictCacheManager;
import org.itsuka.dict.cache.LocalDictCacheConfiguration;
import org.itsuka.dict.cache.RedisDictCacheConfiguration;
import org.itsuka.dict.interceptor.DictInterceptorConfiguration;
import org.itsuka.dict.loader.DatasourceDictLoader;
import org.itsuka.dict.loader.DictLoader;
import org.itsuka.dict.properties.DictProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(DictProperties.class)
@ConditionalOnProperty(prefix = "itsuka.dict", value = "enable", havingValue = "true", matchIfMissing = true)
@Import({LocalDictCacheConfiguration.class, RedisDictCacheConfiguration.class, DictInterceptorConfiguration.class})
public class DictAutoConfiguration {

    /**
     * 字典数据加载器运行器
     * 
     * @param dObjectProvider 字典数据加载器对象提供器
     * @return 字典数据加载器运行器
     */
    @Bean
    @ConditionalOnProperty(prefix = "itsuka.dict", value = "autoLoad", havingValue = "true", matchIfMissing = false)
    public DictCacheLoadRunner dictCacheLoader(ObjectProvider<DictLoader> dObjectProvider) {
        return new DictCacheLoadRunner(dObjectProvider.getIfAvailable());
    }

    /**
     * 字典数据加载器
     * 
     * @param properties 字典属性
     * @param dictCacheManager 字典缓存管理器
     * @return 字典数据加载器
     */
    @Bean
    @ConditionalOnMissingBean(DictLoader.class)
    public DictLoader dictLoader(DictProperties properties, DictCacheManager dictCacheManager) {
        return new DatasourceDictLoader(properties, dictCacheManager);
    }
}
