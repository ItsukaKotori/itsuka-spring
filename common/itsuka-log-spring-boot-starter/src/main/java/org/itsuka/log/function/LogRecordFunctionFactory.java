package org.itsuka.log.function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes {@link LogRecordFunction} instances by their {@link LogRecordFunction#functionName() name}.
 *
 * <p>Duplicate names are rejected eagerly so that a misconfigured application fails fast at
 * startup instead of silently rendering values with an arbitrary function.</p>
 */
public class LogRecordFunctionFactory {

    private final Map<String, LogRecordFunction> functions;

    /**
     * Creates a factory from the given functions.
     *
     * @param functions the functions to register, may be empty
     * @throws IllegalStateException when two functions share the same name
     */
    public LogRecordFunctionFactory(List<LogRecordFunction> functions) {
        Map<String, LogRecordFunction> registered = new HashMap<>();
        for (LogRecordFunction function : functions) {
            LogRecordFunction previous = registered.putIfAbsent(function.functionName(), function);
            if (previous != null) {
                throw new IllegalStateException("Duplicate log record function name: " + function.functionName());
            }
        }
        this.functions = Map.copyOf(registered);
    }

    /**
     * Returns the function registered under the given name.
     *
     * @param name the function name
     * @return the function, or {@code null} when no function is registered under that name
     */
    public LogRecordFunction get(String name) {
        return this.functions.get(name);
    }
}
