package org.itsuka.log.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds per-thread operation log variables, scoped to nested spans.
 */
public final class LogRecordContext {

    private static final ThreadLocal<Deque<Map<String, Object>>> CONTEXT =
        new InheritableThreadLocal<>() {
            @Override
            protected Deque<Map<String, Object>> initialValue() {
                return new ArrayDeque<>();
            }

            @Override
            protected Deque<Map<String, Object>> childValue(Deque<Map<String, Object>> parentValue) {
                Deque<Map<String, Object>> copy = new ArrayDeque<>();
                for (Map<String, Object> span : parentValue) {
                    copy.addLast(new LinkedHashMap<>(span));
                }
                return copy;
            }
        };

    private LogRecordContext() {
    }

    /**
     * Starts a new variable scope. Each span keeps its own variables.
     */
    public static void pushSpan() {
        CONTEXT.get().push(new LinkedHashMap<>());
    }

    /**
     * Ends the current variable scope. Does nothing when no span is active.
     */
    public static void popSpan() {
        Deque<Map<String, Object>> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * Stores a variable in the current span, starting one when necessary.
     *
     * @param name  variable name
     * @param value variable value
     */
    public static void putVariable(String name, Object value) {
        Deque<Map<String, Object>> stack = CONTEXT.get();
        if (stack.isEmpty()) {
            stack.push(new LinkedHashMap<>());
        }
        stack.peek().put(name, value);
    }

    /**
     * Returns the variables of the current span, or an empty map when no span is active.
     *
     * @return current span variables
     */
    public static Map<String, Object> getVariables() {
        Deque<Map<String, Object>> stack = CONTEXT.get();
        if (stack.isEmpty()) {
            return Map.of();
        }
        return stack.peek();
    }

    /**
     * Removes all variables and spans for the current thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
