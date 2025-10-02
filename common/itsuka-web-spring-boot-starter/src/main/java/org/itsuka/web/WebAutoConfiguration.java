package org.itsuka.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.itsuka.web.interceptor.PerformanceMonitorInterceptor;
import org.itsuka.web.interceptor.RequestResponseLoggingInterceptor;
import org.itsuka.web.rest.CustomRestTemplate;
import org.itsuka.web.utils.SpringContextHolder;
import org.itsuka.web.utils.WebHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web自动注入类
 * 提供Web相关的基础组件和配置
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@Configuration
@ConditionalOnProperty(prefix = "itsuka.web", value = "enable", matchIfMissing = true)
public class WebAutoConfiguration implements WebMvcConfigurer {

    /**
     * Web Helper
     *
     * @return Web Helper Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public WebHelper webHelper() {
        return new WebHelper();
    }

    /**
     * Spring ContextUtils
     *
     * @return Spring ContextUtils
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    /**
     * 自定义ObjectMapper
     * 配置JSON序列化和反序列化规则
     *
     * @return 自定义ObjectMapper
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 设置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用默认类型信息
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // 注册Java8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        return objectMapper;
    }

    /**
     * 自定义RestTemplate
     *
     * @return RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new CustomRestTemplate();
    }

    /**
     * 请求响应日志拦截器
     *
     * @return RequestResponseLoggingInterceptor
     */
    @Bean
    @ConditionalOnProperty(prefix = "itsuka.web.logging", value = "enabled", havingValue = "true", matchIfMissing = false)
    public RequestResponseLoggingInterceptor requestResponseLoggingInterceptor() {
        return new RequestResponseLoggingInterceptor();
    }

    /**
     * 性能监控拦截器
     *
     * @return PerformanceMonitorInterceptor
     */
    @Bean
    @ConditionalOnProperty(prefix = "itsuka.web.performance", value = "enabled", havingValue = "true", matchIfMissing = false)
    public PerformanceMonitorInterceptor performanceMonitorInterceptor() {
        return new PerformanceMonitorInterceptor();
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册性能监控拦截器
        if (performanceMonitorInterceptor() != null) {
            registry.addInterceptor(performanceMonitorInterceptor())
                    .addPathPatterns("/**")
                    .excludePathPatterns("/actuator/**", "/health", "/error");
        }

        // 注册请求响应日志拦截器
        if (requestResponseLoggingInterceptor() != null) {
            registry.addInterceptor(requestResponseLoggingInterceptor())
                    .addPathPatterns("/**")
                    .excludePathPatterns("/actuator/**", "/health", "/error");
        }
    }
}
