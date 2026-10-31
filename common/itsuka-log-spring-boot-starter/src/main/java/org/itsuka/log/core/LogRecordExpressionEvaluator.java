package org.itsuka.log.core;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.itsuka.log.context.LogRecordContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * Evaluates SpEL expressions used by operation log annotations.
 */
public class LogRecordExpressionEvaluator extends CachedExpressionEvaluator {

    private static final String RESULT_VARIABLE = "_ret";

    private static final String ERROR_MSG_VARIABLE = "_errorMsg";

    private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>(64);

    private final ParameterNameDiscoverer parameterNameDiscoverer = getParameterNameDiscoverer();

    /**
     * Creates an evaluation context exposing method parameters, the return value,
     * the error message and variables of the current {@link LogRecordContext} span.
     *
     * @param method       the annotated method
     * @param args         the method arguments
     * @param targetClass  the target class
     * @param result       the method return value, may be {@code null}
     * @param errorMsg     the caught error message, may be {@code null}
     * @param beanFactory  the bean factory used to resolve {@code @beanName}, may be {@code null}
     * @return the populated evaluation context
     */
    public EvaluationContext createContext(Method method, Object[] args, Class<?> targetClass,
            Object result, String errorMsg, BeanFactory beanFactory) {
        LogRecordEvaluationContext context = new LogRecordEvaluationContext(
            method, args, this.parameterNameDiscoverer);
        for (Map.Entry<String, Object> entry : LogRecordContext.getVariables().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        context.setVariable(RESULT_VARIABLE, result);
        context.setVariable(ERROR_MSG_VARIABLE, errorMsg);
        if (beanFactory != null) {
            context.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }
        return context;
    }

    /**
     * Evaluates the given expression against the given context, caching the parsed expression.
     *
     * @param expression the SpEL expression
     * @param key        the annotated element key used for caching
     * @param context    the evaluation context
     * @return the evaluation result, may be {@code null}
     */
    public Object evaluate(String expression, AnnotatedElementKey key, EvaluationContext context) {
        return getExpression(this.expressionCache, key, expression).getValue(context);
    }

    /**
     * {@link MethodBasedEvaluationContext} that exposes method arguments by parameter name
     * as well as by their {@code aX}/{@code pX} positional aliases.
     */
    private static class LogRecordEvaluationContext extends MethodBasedEvaluationContext {

        LogRecordEvaluationContext(Method method, Object[] args, ParameterNameDiscoverer parameterNameDiscoverer) {
            super(null, method, args, parameterNameDiscoverer);
        }
    }
}
