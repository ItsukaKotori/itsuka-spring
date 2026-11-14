package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class MySqlFlywayIT {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Test
    void migratesMultipleSourcesWithDuplicateVersions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        new ApplicationContextRunner()
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
                "spring.flyway.baseline-version=0")
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
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
            });
    }
}
