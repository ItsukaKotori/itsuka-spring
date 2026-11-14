package org.itsuka.flyway;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 根据 {@code itsuka.flyway} 模式配置决定是否关闭 Spring Boot 原生 Flyway。
 *
 * <p>未配置 source 且未启用原生模式时关闭原生 Flyway，避免默认扫描
 * {@code classpath:db/migration} 与本 starter 的多路径迁移重复执行。</p>
 *
 * @author Itsuka
 */
public class ItsukaFlywayEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DISABLED_PROPERTY_SOURCE_NAME = "itsukaFlywayDefaults";

    static final String CONFLICT_MESSAGE =
        "itsuka.flyway.native-enabled=true 与 itsuka.flyway.sources 不能同时使用；请二选一。";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ItsukaFlywayProperties properties = Binder.get(environment)
            .bind("itsuka.flyway", ItsukaFlywayProperties.class)
            .orElseGet(ItsukaFlywayProperties::new);

        if (properties.isNativeEnabled() && !properties.getSources().isEmpty()) {
            throw new IllegalStateException(CONFLICT_MESSAGE);
        }
        boolean multiSourceWillRun = properties.isEnabled()
            && !properties.isNativeEnabled()
            && !properties.getSources().isEmpty();
        if (properties.isNativeEnabled() || multiSourceWillRun) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(
            DISABLED_PROPERTY_SOURCE_NAME,
            Map.of("spring.flyway.enabled", "false")));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
