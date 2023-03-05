package org.itsuka.mybatis.plus;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mybatis Plus自动配置类
 *
 * @author Itsuka
 * @date 2023/3/5
 */
@Configuration
@ConditionalOnProperty(prefix = "itsuka.mybatis-plus", value = "enable", matchIfMissing = true)
public class MybatisPlusAutoConfiguration {
    /**
     * 拦截器
     *
     * @return 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
