package org.itsuka.log;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.itsuka.log.core.LogRecordAspect;
import org.itsuka.log.query.OperationLogQueryService;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.store.AsyncLogRecordStore;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class LogRecordAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            LogRecordSecurityAutoConfiguration.class,
            LogRecordWebAutoConfiguration.class,
            LogRecordJdbcAutoConfiguration.class,
            LogRecordSlf4jAutoConfiguration.class,
            LogRecordAutoConfiguration.class
        ));

    private ApplicationContextRunner jdbcRunner() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:autoconf;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(dataSource);
        return new ApplicationContextRunner()
            .withBean(DataSource.class, () -> dataSource)
            .withBean(JdbcTemplate.class, () -> jdbcTemplate)
            .withBean(NamedParameterJdbcTemplate.class, () -> namedTemplate)
            .withConfiguration(AutoConfigurations.of(
                LogRecordSecurityAutoConfiguration.class,
                LogRecordWebAutoConfiguration.class,
                LogRecordJdbcAutoConfiguration.class,
                LogRecordSlf4jAutoConfiguration.class,
                LogRecordAutoConfiguration.class
            ))
            .withPropertyValues("itsuka.log.schema-location=classpath:schema-h2.sql");
    }

    @Test
    void disabledRegistersNothing() {
        runner.withPropertyValues("itsuka.log.enabled=false")
            .run(ctx -> {
                assertThat(ctx).doesNotHaveBean(LogRecordAspect.class);
                assertThat(ctx).doesNotHaveBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
            });
    }

    @Test
    void slf4jStorageDoesNotRegisterJdbcBeans() {
        runner.withPropertyValues("itsuka.log.storage=slf4j")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(JdbcLogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(LogSchemaInitializer.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
            });
    }

    @Test
    void defaultJdbcRegistersStoreSchemaAndQuery() {
        jdbcRunner()
            .withPropertyValues("itsuka.log.storage=jdbc")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(JdbcLogRecordStore.class);
                assertThat(ctx).hasSingleBean(LogSchemaInitializer.class);
                assertThat(ctx).hasSingleBean(OperationLogQueryService.class);
            });
    }

    @Test
    void asyncWrapsBuiltInStore() {
        jdbcRunner()
            .withPropertyValues("itsuka.log.async.enabled=true")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx.getBean(LogRecordStore.class)).isInstanceOf(AsyncLogRecordStore.class);
                assertThat(ctx).hasBean("itsukaLogExecutor");
            });
    }

    @Test
    void customStoreBacksOffBuiltIns() {
        LogRecordStore custom = log -> {};
        jdbcRunner()
            .withBean(LogRecordStore.class, () -> custom)
            .withPropertyValues("itsuka.log.async.enabled=true")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx.getBean(LogRecordStore.class)).isSameAs(custom);
                assertThat(ctx).doesNotHaveBean(JdbcLogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(LogSchemaInitializer.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
                assertThat(ctx).doesNotHaveBean(AsyncLogRecordStore.class);
            });
    }

    @Test
    void missingDataSourceFailsFast() {
        runner.withPropertyValues("itsuka.log.storage=jdbc")
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                    .hasStackTraceContaining("LogRecordStore");
            });
    }
}
