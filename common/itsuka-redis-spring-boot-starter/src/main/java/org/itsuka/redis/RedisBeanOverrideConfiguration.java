package org.itsuka.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Bean覆盖配置
 * 自动启用Bean定义覆盖，无需用户手动配置
 * 
 * @author Itsuka
 */
public class RedisBeanOverrideConfiguration implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.main.allow-bean-definition-overriding", "true");
        
        MapPropertySource propertySource = new MapPropertySource("redis-bean-override-config", properties);
        environment.getPropertySources().addLast(propertySource);
    }
}