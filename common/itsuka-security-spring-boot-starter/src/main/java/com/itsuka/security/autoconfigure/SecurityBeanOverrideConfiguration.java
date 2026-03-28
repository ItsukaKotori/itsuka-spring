package com.itsuka.security.autoconfigure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Security Bean覆盖配置
 * 自动启用Bean定义覆盖，确保统一的安全组件配置
 * 
 * @author Itsuka
 */
public class SecurityBeanOverrideConfiguration implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.main.allow-bean-definition-overriding", "true");
        
        MapPropertySource propertySource = new MapPropertySource("security-bean-override-config", properties);
        environment.getPropertySources().addLast(propertySource);
    }
}