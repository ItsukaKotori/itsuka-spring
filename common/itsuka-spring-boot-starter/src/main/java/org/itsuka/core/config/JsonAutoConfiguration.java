package org.itsuka.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.utils.JacksonDeepCopier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * JSON自动配置类
 * 提供统一的ObjectMapper配置，适用于所有模块（包括WebFlux）
 *
 * @author Itsuka
 * @since 2023/12/07
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@Slf4j
public class JsonAutoConfiguration {

    /**
     * 统一的ObjectMapper配置
     * 强制所有项目使用相同的JSON序列化配置，确保数据一致性
     * 适用于Spring MVC和Spring WebFlux
     *
     * @return JsonMapperBuilderCustomizer
     */
    @Bean
    JsonMapperBuilderCustomizer jacksonCustomizer() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        return builder -> builder
                .changeDefaultVisibility(visibilityChecker -> {
                    visibilityChecker.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
                    return visibilityChecker;
                })
                .addModules(simpleModule);
    }

    /**
     * 自定义的Jackson深拷贝器
     * 提供类型安全的深拷贝功能，避免类型转换问题
     *
     * @param jsonMapper 用于序列化和反序列化的JsonMapper
     * @return 自定义的JacksonDeepCopyer实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(JacksonDeepCopier.class)
    public JacksonDeepCopier jacksonDeepCopier(JsonMapper jsonMapper) {
        return new JacksonDeepCopier(jsonMapper);
    }
}