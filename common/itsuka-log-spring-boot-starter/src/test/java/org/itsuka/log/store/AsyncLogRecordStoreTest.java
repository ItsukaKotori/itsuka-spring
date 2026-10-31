package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.support.LogRecordAsyncExecutor;
import org.junit.jupiter.api.Test;
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
}
