package org.itsuka.log.core;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.itsuka.log.annotation.LogRecord;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Parses {@link LogRecord} annotations into {@link LogRecordOperation} values.
 *
 * <p>Results are cached per {@link Method}; methods without the annotation are cached as
 * {@link Optional#empty()}.</p>
 */
public class LogRecordOperationParser {

    private final ConcurrentMap<Method, Optional<LogRecordOperation>> cache = new ConcurrentHashMap<>();

    /**
     * Parses the annotation on the given method.
     *
     * @param method the method to inspect
     * @return the parsed operation, or {@link Optional#empty()} when the method is not annotated
     */
    public Optional<LogRecordOperation> parse(Method method) {
        return this.cache.computeIfAbsent(method, LogRecordOperationParser::resolve);
    }

    private static Optional<LogRecordOperation> resolve(Method method) {
        LogRecord annotation = AnnotatedElementUtils.findMergedAnnotation(method, LogRecord.class);
        if (annotation == null) {
            return Optional.empty();
        }
        return Optional.of(new LogRecordOperation(
                annotation.success(),
                annotation.fail(),
                annotation.operator(),
                annotation.bizNo(),
                annotation.type(),
                annotation.category(),
                annotation.detail(),
                annotation.condition()));
    }
}
