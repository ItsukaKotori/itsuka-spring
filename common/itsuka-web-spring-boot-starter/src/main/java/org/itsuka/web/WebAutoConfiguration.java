package org.itsuka.web;

import org.itsuka.web.exception.handler.GlobalExceptionHandler;
import org.itsuka.web.interceptor.PerformanceMonitorInterceptor;
import org.itsuka.web.interceptor.RequestResponseLoggingInterceptor;
import org.itsuka.web.rest.CustomRestTemplate;
import org.itsuka.web.utils.SpringContextHolder;
import org.itsuka.web.utils.WebHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@AutoConfiguration
@ConditionalOnProperty(prefix = "itsuka.web", value = "enable", matchIfMissing = true)
@Import(GlobalExceptionHandler.class)
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
        // 性能监控拦截器
        try {
            PerformanceMonitorInterceptor perfInterceptor = performanceMonitorInterceptor();
            if (perfInterceptor != null) {
                registry.addInterceptor(perfInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns("/actuator/**", "/health", "/error");
            }
        } catch (Exception e) {
            // 如果性能监控拦截器没有启用，就忽略
        }

        // 请求响应日志拦截器
        try {
            RequestResponseLoggingInterceptor logInterceptor = requestResponseLoggingInterceptor();
            if (logInterceptor != null) {
                registry.addInterceptor(logInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns("/actuator/**", "/health", "/error");
            }
        } catch (Exception e) {
            // 如果请求响应日志拦截器没有启用，就忽略
        }
    }
}
