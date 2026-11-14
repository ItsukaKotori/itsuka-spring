package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.DefaultResourceLoader;

class ItsukaFlywayFactoryTest {

    @Test
    void mapsCommonPropertiesAndSourceOverrides() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        FlywayProperties flywayProperties = new FlywayProperties();
        flywayProperties.setInstalledBy("migration-test");
        flywayProperties.setOutOfOrder(true);
        flywayProperties.setCleanDisabled(false);
        flywayProperties.setValidateOnMigrate(false);
        flywayProperties.setTablespace("test_ts");
        flywayProperties.setPlaceholders(Map.of("app_table", "flyway_placeholder_test"));

        ItsukaFlywayProperties starterProperties = new ItsukaFlywayProperties();
        starterProperties.setFailOnMissingLocations(true);

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            flywayProperties, starterProperties, new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(), List.of(), List.of());

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("migration-test");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isTrue();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isFalse();
        assertThat(flyway.getConfiguration().isValidateOnMigrate()).isFalse();
        assertThat(flyway.getConfiguration().getTablespace()).isEqualTo("test_ts");
        assertThat(flyway.getConfiguration().getPlaceholders())
            .containsEntry("app_table", "flyway_placeholder_test");
        assertThat(flyway.getConfiguration().getTable()).isEqualTo("flyway_schema_history_order");
        assertThat(flyway.getConfiguration().getLocations())
            .extracting(Location::getDescriptor)
            .containsExactly("classpath:flyway/order");
        assertThat(flyway.getConfiguration().isFailOnMissingLocations()).isTrue();
    }

    @Test
    void appliesCustomizerBeforeSourceLocationAndTable() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory-customizer;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        FlywayConfigurationCustomizer customizer = configuration -> {
            configuration.installedBy("custom");
            configuration.table("ignored_by_source");
            configuration.locations("classpath:ignored");
        };

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            new FlywayProperties(), new ItsukaFlywayProperties(), new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(customizer), List.of(), List.of());

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getInstalledBy()).isEqualTo("custom");
        assertThat(flyway.getConfiguration().getTable()).isEqualTo("flyway_schema_history_order");
        assertThat(flyway.getConfiguration().getLocations())
            .extracting(Location::getDescriptor)
            .containsExactly("classpath:flyway/order");
    }

    @Test
    void expandsVendorPlaceholderInCallbackLocations() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory-vendor;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        FlywayProperties flywayProperties = new FlywayProperties();
        flywayProperties.setCallbackLocations(List.of("classpath:db/callback/{vendor}"));

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            flywayProperties, new ItsukaFlywayProperties(), new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(), List.of(), List.of());

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getCallbackLocations())
            .extracting(Location::getDescriptor)
            .containsExactly("classpath:db/callback/" + DatabaseDriver.H2.getId());
    }

    @Test
    void wiresCallbacksAndJavaMigrations() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:factory-extensions;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        Callback callback = new BaseCallback() {
            @Override
            public void handle(org.flywaydb.core.api.callback.Event event,
                    org.flywaydb.core.api.callback.Context context) {
            }
        };
        JavaMigration javaMigration = new JavaMigration() {
            @Override
            public MigrationVersion getVersion() {
                return MigrationVersion.fromVersion("3");
            }

            @Override
            public String getDescription() {
                return "java migration";
            }

            @Override
            public Integer getChecksum() {
                return 0;
            }

            @Override
            public boolean canExecuteInTransaction() {
                return true;
            }

            @Override
            public void migrate(Context context) {
            }
        };

        ItsukaFlywayFactory factory = new ItsukaFlywayFactory(
            new FlywayProperties(), new ItsukaFlywayProperties(), new DefaultResourceLoader(),
            (sourceName) -> dataSource, List.of(), List.of(javaMigration), List.of(callback));

        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setLocation("classpath:flyway/order");

        Flyway flyway = factory.create(source);

        assertThat(flyway.getConfiguration().getCallbacks()).contains(callback);
        assertThat(flyway.getConfiguration().getJavaMigrations()).contains(javaMigration);
    }
}
