package org.itsuka.log;

import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.store.AsyncLogRecordStore;
import org.itsuka.log.store.Slf4jLogRecordStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Registers the SLF4J-backed store when the storage is {@code slf4j}.
 *
 * <p>The class-level {@code @ConditionalOnMissingBean(LogRecordStore.class)} lets a user-defined
 * store disable this built-in store.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "itsuka.log.storage", havingValue = "slf4j")
@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(LogRecordStore.class)
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordSlf4jAutoConfiguration {

    @Bean
    LogRecordStore slf4jLogRecordStore(LogRecordProperties properties,
            @Qualifier("itsukaLogExecutor") ObjectProvider<ThreadPoolTaskExecutor> asyncExecutor) {
        Slf4jLogRecordStore store = new Slf4jLogRecordStore();
        if (properties.getAsync().isEnabled()) {
            return new AsyncLogRecordStore(store, asyncExecutor.getObject());
        }
        return store;
    }
}
