package org.itsuka.mybatis.plus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.itsuka.mybatis.plus.handler.DefaultFieldHandler;
import org.itsuka.mybatis.plus.interceptor.DataPermissionInterceptor;
import org.itsuka.mybatis.plus.properties.DataPermissionProperty;
import org.itsuka.web.utils.WebHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mybatis Plus自动配置类
 *
 * @author Itsuka
 * @date 2023/3/5
 */
@Configuration
@EnableConfigurationProperties({DataPermissionProperty.class})
@ConditionalOnProperty(prefix = "itsuka.mybatis-plus", value = "enable", matchIfMissing = true)
public class MybatisPlusAutoConfiguration {
    /**
     * 拦截器
     *
     * @return 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataPermissionProperty property, WebHelper webHelper) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(property, webHelper));
        return interceptor;
    }

    /**
     * 自动填充类 Bean
     *
     * @param helper helper
     * @return 自动填充类 Bean
     */
    @Bean
    public MetaObjectHandler metaObjectHandler(WebHelper helper) {
        return new DefaultFieldHandler(helper);
    }
}
