package org.itsuka.log;

import javax.sql.DataSource;

import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.query.JdbcOperationLogQueryService;
import org.itsuka.log.query.OperationLogQueryService;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.store.AsyncLogRecordStore;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registers the JDBC-backed store, schema initializer and query service when a single
 * {@link DataSource} is available and the storage is {@code jdbc}.
 *
 * <p>The class-level {@code @ConditionalOnMissingBean(LogRecordStore.class)} lets a user-defined
 * store disable the built-in store, schema initializer and query service.</p>
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration"
})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(name = "itsuka.log.storage", havingValue = "jdbc", matchIfMissing = true)
@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnMissingBean(LogRecordStore.class)
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordJdbcAutoConfiguration {

    @Bean
    LogRecordStore jdbcLogRecordStore(JdbcTemplate jdbcTemplate, LogRecordProperties properties,
            ObjectProvider<PlatformTransactionManager> transactionManager,
            @Qualifier("itsukaLogExecutor") ObjectProvider<ThreadPoolTaskExecutor> asyncExecutor) {
        JdbcLogRecordStore store = new JdbcLogRecordStore(jdbcTemplate, properties,
                transactionTemplate(properties, transactionManager));
        if (properties.getAsync().isEnabled()) {
            return new AsyncLogRecordStore(store, asyncExecutor.getObject());
        }
        return store;
    }

    @Bean
    @ConditionalOnProperty(prefix = "itsuka.log", name = "auto-init-schema",
            havingValue = "true", matchIfMissing = true)
    LogSchemaInitializer logSchemaInitializer(DataSource dataSource, LogRecordProperties properties,
            ResourceLoader resourceLoader) {
        return new LogSchemaInitializer(dataSource, properties, resourceLoader);
    }

    @Bean
    @ConditionalOnProperty(prefix = "itsuka.log.query", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(OperationLogQueryService.class)
    OperationLogQueryService jdbcOperationLogQueryService(NamedParameterJdbcTemplate jdbcTemplate,
            LogRecordProperties properties) {
        return new JdbcOperationLogQueryService(jdbcTemplate, properties);
    }

    private TransactionTemplate transactionTemplate(LogRecordProperties properties,
            ObjectProvider<PlatformTransactionManager> transactionManager) {
        PlatformTransactionManager txManager = transactionManager.getIfAvailable();
        if (txManager == null || !properties.getTransaction().isRequiresNew()) {
            return null;
        }
        TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
