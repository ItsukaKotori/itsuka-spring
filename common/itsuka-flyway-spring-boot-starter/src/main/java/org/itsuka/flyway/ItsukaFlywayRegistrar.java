package org.itsuka.flyway;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 为每个 source 注册独立的 {@link Flyway} 与 {@link FlywayMigrationInitializer} Bean。
 *
 * <p>每个 source 使用 {@code flyway_<normalized>} 与 {@code flywayInitializer_<normalized>}
 * 命名，初始化器通过 {@code dependsOn} 与 {@code order} 按配置顺序依次执行。</p>
 *
 * @author Itsuka
 */
final class ItsukaFlywayRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private Environment environment;

    private BeanFactory beanFactory;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ItsukaFlywayProperties properties = Binder.get(this.environment)
            .bind("itsuka.flyway", ItsukaFlywayProperties.class)
            .orElseGet(ItsukaFlywayProperties::new);
        ItsukaFlywayPropertiesValidator.validate(properties);

        List<ItsukaFlywayProperties.Source> sources = properties.getSources();
        if (sources == null) {
            return;
        }
        String previousInitializer = null;
        for (int i = 0; i < sources.size(); i++) {
            ItsukaFlywayProperties.Source source = sources.get(i);
            String normalized = source.normalizedName();

            RootBeanDefinition flywayDefinition = new RootBeanDefinition(Flyway.class);
            flywayDefinition.setInstanceSupplier(() -> {
                ItsukaFlywayFactory factory =
                    this.beanFactory.getBean("itsukaFlywayFactory", ItsukaFlywayFactory.class);
                return factory.create(source);
            });
            registry.registerBeanDefinition("flyway_" + normalized, flywayDefinition);

            RootBeanDefinition initializerDefinition =
                new RootBeanDefinition(FlywayMigrationInitializer.class);
            initializerDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
                0, new RuntimeBeanReference("flyway_" + normalized));
            initializerDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
                1, migrationStrategy());
            initializerDefinition.getPropertyValues().add("order", i);
            if (previousInitializer != null) {
                initializerDefinition.setDependsOn(previousInitializer);
            }
            String initializerName = "flywayInitializer_" + normalized;
            registry.registerBeanDefinition(initializerName, initializerDefinition);
            previousInitializer = initializerName;
        }
    }

    private FlywayMigrationStrategy migrationStrategy() {
        return (flyway) -> {
            FlywayMigrationStrategy userStrategy =
                this.beanFactory.getBeanProvider(FlywayMigrationStrategy.class).getIfAvailable();
            if (userStrategy != null) {
                userStrategy.migrate(flyway);
            }
            else {
                flyway.migrate();
            }
        };
    }
}
