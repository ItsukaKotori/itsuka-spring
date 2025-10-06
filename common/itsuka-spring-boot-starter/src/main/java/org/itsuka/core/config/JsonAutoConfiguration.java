package org.itsuka.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
     * @return 自定义ObjectMapper
     */
    @Bean("objectMapper")
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        log.info("Initializing unified ObjectMapper configuration");
        
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 设置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用默认类型信息
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // 注册Java8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        log.info("ObjectMapper configuration completed with JavaTimeModule and default typing");
        
        return objectMapper;
    }
}