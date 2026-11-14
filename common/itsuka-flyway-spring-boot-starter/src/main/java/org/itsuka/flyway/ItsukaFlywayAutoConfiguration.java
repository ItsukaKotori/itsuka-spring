package org.itsuka.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

/**
 * 多路径 Flyway 迁移自动配置。
 *
 * @author Itsuka
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnMissingBean(Flyway.class)
@ConditionalOnBooleanProperty(prefix = "itsuka.flyway", name = "enabled", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = "itsuka.flyway", name = "native-enabled",
    havingValue = false, matchIfMissing = true)
@EnableConfigurationProperties({ItsukaFlywayProperties.class, FlywayProperties.class})
@Import({ItsukaFlywayRegistrar.class, DatabaseInitializationDependencyConfigurer.class})
public class ItsukaFlywayAutoConfiguration {

    @Bean
    ItsukaFlywayFactory itsukaFlywayFactory(
            FlywayProperties flywayProperties,
            ItsukaFlywayProperties itsukaFlywayProperties,
            ResourceLoader resourceLoader,
            ObjectProvider<DataSource> dataSource,
            @FlywayDataSource ObjectProvider<DataSource> flywayDataSource,
            ObjectProvider<FlywayConnectionDetails> flywayConnectionDetails,
            ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails,
            ObjectProvider<FlywayConfigurationCustomizer> configurationCustomizers,
            ObjectProvider<JavaMigration> javaMigrations,
            ObjectProvider<Callback> callbacks) {
        return new ItsukaFlywayFactory(
            flywayProperties,
            itsukaFlywayProperties,
            resourceLoader,
            ItsukaFlywayDataSourceResolver.of(
                flywayProperties, dataSource, flywayDataSource,
                flywayConnectionDetails, jdbcConnectionDetails),
            configurationCustomizers.orderedStream().toList(),
            javaMigrations.orderedStream().toList(),
            callbacks.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean(name = "itsukaFlywaySchemaManagementProvider")
    SchemaManagementProvider itsukaFlywaySchemaManagementProvider(ObjectProvider<Flyway> flyways) {
        return new ItsukaFlywaySchemaManagementProvider(flyways);
    }
}
