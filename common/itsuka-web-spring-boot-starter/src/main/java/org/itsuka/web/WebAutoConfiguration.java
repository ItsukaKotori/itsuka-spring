package org.itsuka.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web自动注入类
 *
 * @author Itsuka
 * @date 2023/2/26
 */
@Configuration
@ConditionalOnProperty(prefix = "itsuka.web", value = "enable", matchIfMissing = true)
public class WebAutoConfiguration {
    /**
     * Web Helper
     *
     * @return Web Helper Bean
     */
    @Bean
    public WebHelper webHelper() {
        return new WebHelper();
    }

    @Bean
    public SpringContextHolderUtils springContextHolderUtils() {
        return new SpringContextHolderUtils();
    }
}
