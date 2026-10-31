package org.itsuka.log.support;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.itsuka.log.properties.LogRecordProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Factory building the {@link ThreadPoolTaskExecutor} used by asynchronous operation log writes.
 */
public final class LogRecordAsyncExecutor {

    private LogRecordAsyncExecutor() {
    }

    /**
     * Creates an uninitialized executor from the given asynchronous properties.
     *
     * <p>The caller is responsible for initializing the executor (directly or through
     * {@code afterPropertiesSet}), so it can be used both standalone and as a Spring bean.</p>
     *
     * @param properties the asynchronous write configuration
     * @return the configured, not yet initialized executor
     */
    public static ThreadPoolTaskExecutor create(LogRecordProperties.Async properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds((int) properties.getKeepAlive().toSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds((int) properties.getAwaitTermination().toSeconds());
        executor.setRejectedExecutionHandler(rejectionHandler(properties.getRejectionPolicy()));
        return executor;
    }

    private static RejectedExecutionHandler rejectionHandler(LogRecordProperties.RejectionPolicy policy) {
        return switch (policy) {
            case CALLER_RUNS -> new CallerRunsRejectOnShutdownHandler();
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
        };
    }

    /**
     * Runs the task inline while the executor is still active, and throws
     * {@link RejectedExecutionException} once it is shut down.
     *
     * <p>Unlike the JDK {@link ThreadPoolExecutor.CallerRunsPolicy}, which silently drops the task
     * after shutdown, throwing lets {@link org.itsuka.log.store.AsyncLogRecordStore} observe and
     * log the rejection instead of losing the write.</p>
     */
    private static final class CallerRunsRejectOnShutdownHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                r.run();
                return;
            }
            throw new RejectedExecutionException(
                    "Task " + r + " rejected from " + executor + ": executor is shut down");
        }
    }
}
