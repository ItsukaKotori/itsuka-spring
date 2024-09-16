package org.itsuka.mybatis.plus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.itsuka.mybatis.plus.handler.DefaultFieldHandler;
import org.itsuka.mybatis.plus.helper.MybatisPlusHelper;
import org.itsuka.mybatis.plus.permission.DataPermissionEscaper;
import org.itsuka.mybatis.plus.permission.DataPermissionInterceptor;
import org.itsuka.mybatis.plus.permission.DataPermissionProperty;
import org.itsuka.web.utils.WebHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mybatis Plus自动配置类
 *
 * @author Itsuka
 * @since 2023/3/5
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
    public MybatisPlusInterceptor mybatisPlusInterceptor(
        DataPermissionProperty property, WebHelper webHelper, 
        DataPermissionEscaper escaper, MybatisPlusHelper mybatisPlusHelper) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(property, webHelper, escaper, mybatisPlusHelper));
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

    /**
     * 数据权限逃离器
     * 
     * @return
     */
    @Bean
    public DataPermissionEscaper escaper() {
        return new DataPermissionEscaper();
    }

    /**
     * MybatisPlusHelper
     * 
     * @return
     */
    @Bean
    public MybatisPlusHelper mybatisPlusHelper() {
        return new MybatisPlusHelper();
    }
}
