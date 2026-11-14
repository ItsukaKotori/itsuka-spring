package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class ItsukaFlywayEnvironmentPostProcessorIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    private static SpringApplication application() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    private static String[] dataSourceArgs() {
        return new String[] {
            "--spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "--spring.datasource.driver-class-name=org.h2.Driver",
            "--spring.datasource.username=sa",
            "--spring.datasource.password="
        };
    }

    @Test
    void noSourcesDisablesNativeFlyway() {
        try (ConfigurableApplicationContext context = application().run(dataSourceArgs())) {
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled"))
                .isEqualTo("false");
            assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
            // 该 Bean 只能由 AutoConfiguration.imports 加载的 starter 自动配置注册，
            // 用于证明真实启动路径确实加载了自动配置（而非仅由测试直接拼装）。
            assertThat(context.containsBean("itsukaFlywaySchemaManagementProvider")).isTrue();
        }
    }

    @Test
    void nativeModeRunsBootFlyway() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.native-enabled=true",
                "--spring.flyway.locations=classpath:db/migration"))
            .toArray(String[]::new);
        try (ConfigurableApplicationContext context = application().run(args)) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_native_test", Integer.class)).isZero();
        }
    }

    @Test
    void nativeModeWorksWhenStarterIsDisabled() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.enabled=false",
                "--itsuka.flyway.native-enabled=true",
                "--spring.flyway.locations=classpath:db/migration"))
            .toArray(String[]::new);
        try (ConfigurableApplicationContext context = application().run(args)) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_native_test", Integer.class)).isZero();
        }
    }

    @Test
    void nativeModeWithSourcesFailsFast() {
        String[] args = Stream.concat(
            Arrays.stream(dataSourceArgs()),
            Stream.of(
                "--itsuka.flyway.native-enabled=true",
                "--itsuka.flyway.sources[0].name=order",
                "--itsuka.flyway.sources[0].location=classpath:flyway/order"))
            .toArray(String[]::new);
        assertThatThrownBy(() -> application().run(args))
            .hasStackTraceContaining("二选一");
    }
}
