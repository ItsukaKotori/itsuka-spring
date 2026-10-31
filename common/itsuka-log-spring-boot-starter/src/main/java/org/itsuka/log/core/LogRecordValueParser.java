package org.itsuka.log.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.itsuka.log.function.LogRecordFunction;
import org.itsuka.log.function.LogRecordFunctionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

/**
 * Renders operation log templates such as {@code "订单{{#param.orderNo}}"}.
 *
 * <p>A template may reference an expression directly ({@code {{#expr}}}) or pass it through a
 * custom function ({@code {UPPER{#expr}}}). Functions opting into
 * {@link LogRecordFunction#executeBefore()} can be evaluated before the target method runs and
 * cached, keyed by the full template match.</p>
 */
public class LogRecordValueParser {

    private static final Logger log = LoggerFactory.getLogger(LogRecordValueParser.class);

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\s*(\\w*)\\s*\\{([^{}]*?)}}");

    private static final String RESULT_EXPRESSION = "#_ret";

    private static final String ERROR_EXPRESSION = "#_errorMsg";

    private final LogRecordExpressionEvaluator evaluator;

    private final LogRecordFunctionFactory functionFactory;

    public LogRecordValueParser(LogRecordExpressionEvaluator evaluator, LogRecordFunctionFactory functionFactory) {
        this.evaluator = evaluator;
        this.functionFactory = functionFactory;
    }

    /**
     * Renders every template match, substituting cached before-function values when available.
     *
     * @param template     the template text, may be {@code null} or empty
     * @param context      the evaluation context
     * @param key          the annotated element key used for expression caching
     * @param beforeValues values captured by {@link #parseBeforeFunctions}, keyed by full match
     * @return the rendered text, empty when nothing could be evaluated
     */
    public String parse(String template, EvaluationContext context, AnnotatedElementKey key,
            Map<String, String> beforeValues) {
        if (!StringUtils.hasLength(template)) {
            return template;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String replacement;
            if (beforeValues != null && beforeValues.containsKey(match)) {
                replacement = nullToEmpty(beforeValues.get(match));
            } else {
                replacement = render(matcher.group(1), matcher.group(2), context, key);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Evaluates all before-invocation functions found in the given templates.
     *
     * @param templates the templates to scan
     * @param context   the evaluation context
     * @param key       the annotated element key used for expression caching
     * @return captured values keyed by the full template match
     */
    public Map<String, String> parseBeforeFunctions(Collection<String> templates, EvaluationContext context,
            AnnotatedElementKey key) {
        Map<String, String> beforeValues = new HashMap<>();
        for (String template : templates) {
            if (!StringUtils.hasLength(template)) {
                continue;
            }
            Matcher matcher = TEMPLATE_PATTERN.matcher(template);
            while (matcher.find()) {
                String name = matcher.group(1);
                String expression = matcher.group(2);
                if (name.isEmpty() || expression.contains(RESULT_EXPRESSION)
                        || expression.contains(ERROR_EXPRESSION)) {
                    continue;
                }
                LogRecordFunction function = this.functionFactory.get(name);
                if (function == null || !function.executeBefore()) {
                    continue;
                }
                Object value = this.evaluator.evaluate(expression, key, context);
                beforeValues.put(matcher.group(), function.apply(value));
            }
        }
        return beforeValues;
    }

    /**
     * Evaluates a conditional expression controlling whether a log is recorded.
     *
     * @param condition the SpEL condition, optionally wrapped in {@code {{ }}}
     * @param context   the evaluation context
     * @param key       the annotated element key used for expression caching
     * @return {@code true} only when the condition evaluates to {@link Boolean#TRUE}
     */
    public boolean evaluateCondition(String condition, EvaluationContext context, AnnotatedElementKey key) {
        if (!StringUtils.hasLength(condition)) {
            return true;
        }
        String expression = condition.trim();
        if (expression.startsWith("{{") && expression.endsWith("}}")) {
            expression = expression.substring(2, expression.length() - 2).trim();
        }
        return Boolean.TRUE.equals(this.evaluator.evaluate(expression, key, context));
    }

    /**
     * Evaluates a template expected to contain a single expression.
     *
     * @param template the template text
     * @param context  the evaluation context
     * @param key      the annotated element key used for expression caching
     * @return the raw evaluated value when the whole template is one match, otherwise the rendered text
     */
    public Object parseSingleExpression(String template, EvaluationContext context, AnnotatedElementKey key) {
        if (!StringUtils.hasLength(template)) {
            return template;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        if (matcher.matches()) {
            return renderValue(matcher.group(1), matcher.group(2), context, key);
        }
        return parse(template, context, key, Map.of());
    }

    private String render(String name, String expression, EvaluationContext context, AnnotatedElementKey key) {
        Object value = renderValue(name, expression, context, key);
        return value == null ? "" : String.valueOf(value);
    }

    private Object renderValue(String name, String expression, EvaluationContext context, AnnotatedElementKey key) {
        Object value = this.evaluator.evaluate(expression, key, context);
        if (name.isEmpty()) {
            return value;
        }
        LogRecordFunction function = this.functionFactory.get(name);
        if (function == null) {
            log.warn("No log record function registered under name [{}], falling back to the raw value", name);
            return value;
        }
        return function.apply(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
