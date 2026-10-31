package org.itsuka.log.store;

import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.spi.LogRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/**
 * {@link LogRecordStore} decorator submitting writes to an {@link AsyncTaskExecutor}.
 *
 * <p>Rejections and runtime failures raised while submitting the task are logged and never
 * propagated to the caller, so asynchronous logging cannot affect the business result.</p>
 */
public class AsyncLogRecordStore implements LogRecordStore {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogRecordStore.class);

    private final LogRecordStore delegate;

    private final AsyncTaskExecutor executor;

    /**
     * Creates a store delegating writes to the given executor.
     *
     * @param delegate the store performing the actual write
     * @param executor the executor running the write asynchronously
     */
    public AsyncLogRecordStore(LogRecordStore delegate, AsyncTaskExecutor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public void save(OperationLog operationLog) {
        try {
            this.executor.execute(() -> this.delegate.save(operationLog));
        } catch (TaskRejectedException ex) {
            log.error("operation log rejected by async executor, bizNo={}", operationLog.getBizNo(), ex);
        } catch (RuntimeException ex) {
            log.error("operation log async dispatch failed, bizNo={}", operationLog.getBizNo(), ex);
        }
    }
}
