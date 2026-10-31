package org.itsuka.log.core;

/**
 * Result of executing the target method captured by {@link LogRecordAspect}.
 *
 * @param success   whether the method returned normally
 * @param result    the return value, {@code null} on failure
 * @param throwable the thrown exception, {@code null} on success
 */
public record MethodExecuteResult(boolean success, Object result, Throwable throwable) {
}
