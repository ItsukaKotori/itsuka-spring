package org.itsuka.log.function;

/**
 * SPI for customizing values rendered by operation log templates.
 *
 * <p>Functions are referenced from templates as {@code {NAME{{#expression}}}}.
 * A function whose {@link #executeBefore()} returns {@code true} is evaluated before the
 * target method runs, which is useful for capturing state that the method mutates.</p>
 */
public interface LogRecordFunction {

    /**
     * Returns the unique name used to reference this function in templates.
     *
     * @return the function name
     */
    String functionName();

    /**
     * Whether this function must be evaluated before the target method is invoked.
     *
     * @return {@code true} to evaluate before invocation, {@code false} (default) otherwise
     */
    default boolean executeBefore() {
        return false;
    }

    /**
     * Converts the evaluated expression value into the rendered text.
     *
     * @param value the evaluated expression value, may be {@code null}
     * @return the rendered value, may be {@code null}
     */
    String apply(Object value);
}
