package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

class ItsukaFlywayAutoConfigurationTest {

    private JdbcDataSource dataSource;

    private ApplicationContextRunner runner() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ItsukaFlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> dataSource)
            .withPropertyValues(
                "itsuka.flyway.sources[0].name=order",
                "itsuka.flyway.sources[0].location=classpath:flyway/order",
                "itsuka.flyway.sources[1].name=user",
                "itsuka.flyway.sources[1].location=classpath:flyway/user",
                // 多个 Flyway 实例共享同一 schema 时，后续 source 需要 baseline 才能通过
                // Flyway 的非空 schema 检查；baseline-version=0 保证每个 source 的 V1 仍会执行。
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0");
    }

    @Test
    void migratesEachSourceIntoItsOwnHistoryTable() {
        runner().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasBean("flyway_order");
            assertThat(ctx).hasBean("flyway_user");
            assertThat(ctx).doesNotHaveBean("flyway");
            assertThat(ctx).hasBean("flywayInitializer_order");
            assertThat(ctx).hasBean("flywayInitializer_user");

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_order_test", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_user_test", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history_order where type = 'SQL'",
                Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history_user where type = 'SQL'",
                Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                    + "where lower(table_name) = 'flyway_schema_history'", Integer.class)).isZero();
        });
    }

    @Test
    void bootNativeFlywayBacksOffWhenSourcesAreConfigured() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                ItsukaFlywayAutoConfiguration.class, FlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> {
                dataSource = new JdbcDataSource();
                dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
                return dataSource;
            })
            .withPropertyValues(
                "itsuka.flyway.sources[0].name=order",
                "itsuka.flyway.sources[0].location=classpath:flyway/order")
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                assertThat(ctx).hasBean("flyway_order");
                assertThat(ctx).doesNotHaveBean("flyway");
                assertThat(ctx).doesNotHaveBean("flywayInitializer");
                assertThat(new JdbcTemplate(dataSource).queryForObject(
                    "select count(*) from information_schema.tables "
                        + "where lower(table_name) = 'flyway_schema_history'", Integer.class)).isZero();
            });
    }

    @Test
    void migratesSourcesInConfiguredOrder() {
        runner().withUserConfiguration(RecordingStrategyConfiguration.class)
            .run(ctx -> assertThat(ctx.getBean(RecordingFlywayMigrationStrategy.class).migratedTables)
                .containsExactly("flyway_schema_history_order", "flyway_schema_history_user"));
    }

    @Test
    void appliesCustomizerToEverySource() {
        runner().withUserConfiguration(CustomizerConfiguration.class)
            .run(ctx -> {
                assertThat(ctx.getBean("flyway_order", Flyway.class)
                    .getConfiguration().getInstalledBy()).isEqualTo("custom-installed-by");
                assertThat(ctx.getBean("flyway_user", Flyway.class)
                    .getConfiguration().getInstalledBy()).isEqualTo("custom-installed-by");
            });
    }

    @Test
    void reportsManagedForFlywayDataSource() {
        runner().run(ctx -> {
            SchemaManagementProvider provider = ctx.getBean(
                "itsukaFlywaySchemaManagementProvider", SchemaManagementProvider.class);
            assertThat(provider.getSchemaManagement(dataSource)).isEqualTo(SchemaManagement.MANAGED);
            JdbcDataSource other = new JdbcDataSource();
            other.setURL("jdbc:h2:mem:other-provider");
            assertThat(provider.getSchemaManagement(other)).isEqualTo(SchemaManagement.UNMANAGED);
        });
    }

    @Test
    void reportsManagedEvenWhenNativeFlywayIsDisabled() {
        runner().withPropertyValues("spring.flyway.enabled=false").run(ctx -> {
            assertThat(ctx).hasBean("flyway_order");
            SchemaManagementProvider provider = ctx.getBean(
                "itsukaFlywaySchemaManagementProvider", SchemaManagementProvider.class);
            assertThat(provider.getSchemaManagement(dataSource)).isEqualTo(SchemaManagement.MANAGED);
        });
    }

    @Test
    void appliesCommonSpringFlywayProperties() {
        runner().withPropertyValues(
                "spring.flyway.installed-by=spring-boot-config",
                "spring.flyway.out-of-order=true",
                "spring.flyway.validate-on-migrate=false")
            .run(ctx -> {
                Flyway flyway = ctx.getBean("flyway_order", Flyway.class);
                assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("spring-boot-config");
                assertThat(flyway.getConfiguration().isOutOfOrder()).isTrue();
                assertThat(flyway.getConfiguration().isValidateOnMigrate()).isFalse();
                assertThat(new JdbcTemplate(dataSource).queryForObject(
                    "select distinct installed_by from flyway_schema_history_order",
                    String.class)).isEqualTo("spring-boot-config");
            });
    }

    @Test
    void disabledStarterRegistersNoFlywayBeans() {
        runner().withPropertyValues("itsuka.flyway.enabled=false")
            .run(ctx -> {
                assertThat(ctx).doesNotHaveBean("flyway_order");
                assertThat(ctx).doesNotHaveBean("flyway_user");
                assertThat(new JdbcTemplate(dataSource).queryForObject(
                    "select count(*) from information_schema.tables "
                        + "where lower(table_name) = 'flyway_order_test'", Integer.class)).isZero();
            });
    }

    @Test
    void nativeModeUsesBootAutoConfiguration() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                ItsukaFlywayAutoConfiguration.class, FlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> {
                dataSource = new JdbcDataSource();
                dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
                return dataSource;
            })
            .withPropertyValues(
                "itsuka.flyway.native-enabled=true",
                "spring.flyway.locations=classpath:db/migration")
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                assertThat(ctx).hasSingleBean(Flyway.class);
                assertThat(ctx).hasBean("flyway");
                assertThat(new JdbcTemplate(dataSource).queryForObject(
                    "select count(*) from flyway_native_test", Integer.class)).isEqualTo(0);
            });
    }

    @TestConfiguration
    static class RecordingStrategyConfiguration {

        @Bean
        RecordingFlywayMigrationStrategy recordingFlywayMigrationStrategy() {
            return new RecordingFlywayMigrationStrategy();
        }
    }

    static class RecordingFlywayMigrationStrategy implements FlywayMigrationStrategy {

        final List<String> migratedTables = new CopyOnWriteArrayList<>();

        @Override
        public void migrate(Flyway flyway) {
            migratedTables.add(flyway.getConfiguration().getTable());
            flyway.migrate();
        }
    }

    @TestConfiguration
    static class CustomizerConfiguration {

        @Bean
        FlywayConfigurationCustomizer installedByCustomizer() {
            return configuration -> configuration.installedBy("custom-installed-by");
        }
    }
}
