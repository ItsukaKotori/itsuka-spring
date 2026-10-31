package org.itsuka.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.core.LogRecordAspect;
import org.itsuka.log.query.OperationLogQueryService;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class LogRecordAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            LogRecordSecurityAutoConfiguration.class,
            LogRecordWebAutoConfiguration.class,
            LogRecordJdbcAutoConfiguration.class,
            LogRecordSlf4jAutoConfiguration.class,
            LogRecordAutoConfiguration.class
        ));

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
    void customStoreBacksOffBuiltIns() {
        runner.withBean(LogRecordStore.class, () -> log -> {})
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(LogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(JdbcLogRecordStore.class);
                assertThat(ctx).doesNotHaveBean(LogSchemaInitializer.class);
                assertThat(ctx).doesNotHaveBean(OperationLogQueryService.class);
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
