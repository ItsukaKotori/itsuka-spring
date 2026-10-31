package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.support.LogRecordAsyncExecutor;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncLogRecordStoreTest {

    @Test
    void delegatesOnExecutorThread() throws Exception {
        AtomicReference<String> thread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        LogRecordStore delegate = log -> {
            thread.set(Thread.currentThread().getName());
            latch.countDown();
        };
        LogRecordProperties.Async props = new LogRecordProperties.Async();
        props.setThreadNamePrefix("itsuka-log-test-");
        ThreadPoolTaskExecutor executor = LogRecordAsyncExecutor.create(props);
        executor.initialize();
        try {
            new AsyncLogRecordStore(delegate, executor).save(OperationLog.builder()
                .bizNo("NO1").success(true).content("x").createdAt(LocalDateTime.now()).build());
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(thread.get()).startsWith("itsuka-log-test-");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void logsDelegateFailureOnWorkerThreadWithoutPropagating() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AsyncLogRecordStore.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        CountDownLatch failed = new CountDownLatch(1);
        CountDownLatch succeeded = new CountDownLatch(1);
        AtomicBoolean firstCall = new AtomicBoolean(true);
        LogRecordStore delegate = log -> {
            if (firstCall.getAndSet(false)) {
                failed.countDown();
                throw new IllegalStateException("database down");
            }
            succeeded.countDown();
        };
        ThreadPoolTaskExecutor executor = LogRecordAsyncExecutor.create(new LogRecordProperties.Async());
        executor.initialize();
        try {
            AsyncLogRecordStore store = new AsyncLogRecordStore(delegate, executor);

            assertThatCode(() -> store.save(newLog("NO1"))).doesNotThrowAnyException();
            assertThat(failed.await(2, TimeUnit.SECONDS)).isTrue();

            // The worker survived the failure and still processes subsequent writes.
            store.save(newLog("NO2"));
            assertThat(succeeded.await(2, TimeUnit.SECONDS)).isTrue();

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .anySatisfy(event -> {
                    assertThat(event.getFormattedMessage()).contains("NO1");
                    assertThat(event.getThrowableProxy()).isNotNull();
                });
        } finally {
            executor.shutdown();
            logger.detachAppender(appender);
        }
    }

    private static OperationLog newLog(String bizNo) {
        return OperationLog.builder()
            .bizNo(bizNo).success(true).content("x").createdAt(LocalDateTime.now()).build();
    }
}
