package org.itsuka.web.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 脱敏自动配置类
 * 提供脱敏功能的自动配置，基于Jackson序列化实现
 *
 * @author Itsuka
 * @since 2023/12/7
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(DesensitizationProperties.class)
@ConditionalOnProperty(prefix = "itsuka.web.desensitization", value = "enabled", matchIfMissing = true)
public class DesensitizationAutoConfiguration {

    /**
     * 配置脱敏模块
     *
     * @param properties 脱敏配置属性
     * @return 脱敏模块
     */
    @Bean
    @ConditionalOnMissingBean
    public DesensitizationModule desensitizationModule(DesensitizationProperties properties) {
        return new DesensitizationModule(properties);
    }
}