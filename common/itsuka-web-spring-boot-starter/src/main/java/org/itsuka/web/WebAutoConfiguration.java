package org.itsuka.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.itsuka.web.utils.SpringContextHolderUtils;
import org.itsuka.web.utils.WebHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Web自动注入类
 *
 * @author Itsuka
 * @since 2023/2/26
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

    /**
     * Spring ContextUtils
     *
     * @return Spring ContextUtils
     */
    @Bean
    public SpringContextHolderUtils springContextHolderUtils() {
        return new SpringContextHolderUtils();
    }

    /**
     * 自定义ObjectMapper
     *
     * @return 自定义ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }
}
