package org.itsuka.log.core;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.itsuka.log.context.LogRecordContext;
import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogOperatorProvider;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.spi.LogRequestResolver;
import org.itsuka.log.support.DefaultLogOperatorProvider;
import org.itsuka.log.support.LogParameterSerializer;
import org.itsuka.log.support.LogRecordTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.Ordered;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

/**
 * Around advice that records a {@link OperationLog} after an annotated method runs.
 *
 * <p>The advice records success and failure alike, preserves the original business
 * {@link Throwable}, and never lets a logging failure escape to the caller.</p>
 */
@Aspect
public class LogRecordAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(LogRecordAspect.class);

    private final LogRecordProperties properties;

    private final LogRecordOperationParser operationParser;

    private final LogRecordExpressionEvaluator expressionEvaluator;

    private final LogRecordValueParser valueParser;

    private final LogOperatorProvider operatorProvider;

    private final LogRequestResolver requestResolver;

    private final LogParameterSerializer parameterSerializer;

    private final LogRecordStore store;

    /**
     * Creates the aspect wiring together every collaborator it consumes.
     */
    public LogRecordAspect(
            LogRecordProperties properties,
            LogRecordOperationParser operationParser,
            LogRecordExpressionEvaluator expressionEvaluator,
            LogRecordValueParser valueParser,
            LogOperatorProvider operatorProvider,
            LogRequestResolver requestResolver,
            LogParameterSerializer parameterSerializer,
            LogRecordStore store) {
        this.properties = properties;
        this.operationParser = operationParser;
        this.expressionEvaluator = expressionEvaluator;
        this.valueParser = valueParser;
        this.operatorProvider = operatorProvider;
        this.requestResolver = requestResolver;
        this.parameterSerializer = parameterSerializer;
        this.store = store;
    }

    @Around("@annotation(org.itsuka.log.annotation.LogRecord)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> targetClass = pjp.getTarget().getClass();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);

        Optional<LogRecordOperation> parsed;
        try {
            parsed = this.operationParser.parse(method);
        } catch (Exception e) {
            log.warn("Failed to parse operation log annotation for method [{}#{}]: {}",
                    targetClass.getSimpleName(), method.getName(), e.getMessage(), e);
            return pjp.proceed();
        }
        if (parsed.isEmpty()) {
            return pjp.proceed();
        }
        LogRecordOperation operation = parsed.get();
        Object[] args = pjp.getArgs();
        AnnotatedElementKey key = new AnnotatedElementKey(method, targetClass);

        LogRecordContext.pushSpan();
        try {
            Map<String, String> beforeValues;
            try {
                EvaluationContext beforeContext = this.expressionEvaluator.createContext(
                        method, args, targetClass, null, null, null);
                beforeValues = this.valueParser.parseBeforeFunctions(
                        List.of(operation.success(), operation.fail(), operation.bizNo(),
                                operation.detail(), operation.operator()),
                        beforeContext, key);
            } catch (Exception e) {
                log.warn("Failed to evaluate before-functions for method [{}#{}]: {}",
                        targetClass.getSimpleName(), method.getName(), e.getMessage(), e);
                beforeValues = Map.of();
            }

            MethodExecuteResult executeResult;
            try {
                executeResult = new MethodExecuteResult(true, pjp.proceed(), null);
            } catch (Throwable throwable) {
                executeResult = new MethodExecuteResult(false, null, throwable);
            }

            try {
                recordLog(operation, method, targetClass, args, key, executeResult, beforeValues);
            } catch (Exception e) {
                log.warn("Failed to record operation log for method [{}#{}]: {}",
                        targetClass.getSimpleName(), method.getName(), e.getMessage(), e);
            }

            if (!executeResult.success()) {
                throw executeResult.throwable();
            }
            return executeResult.result();
        } finally {
            LogRecordContext.popSpan();
        }
    }

    @Override
    public int getOrder() {
        return this.properties.getAspectOrder();
    }

    private void recordLog(LogRecordOperation operation, Method method, Class<?> targetClass,
            Object[] args, AnnotatedElementKey key, MethodExecuteResult executeResult,
            Map<String, String> beforeValues) {
        boolean success = executeResult.success();
        Object result = executeResult.result();
        Throwable throwable = executeResult.throwable();
        String errorMsg = throwable == null ? null : throwable.getMessage();

        EvaluationContext context = this.expressionEvaluator.createContext(
                method, args, targetClass, result, errorMsg, null);

        if (!this.valueParser.evaluateCondition(operation.condition(), context, key)) {
            return;
        }

        String template = success ? operation.success()
                : (StringUtils.hasText(operation.fail()) ? operation.fail() : operation.success());

        String content = this.valueParser.parse(template, context, key, beforeValues);
        String bizNo = this.valueParser.parse(operation.bizNo(), context, key, beforeValues);
        String detail = this.valueParser.parse(operation.detail(), context, key, beforeValues);

        if (!StringUtils.hasText(bizNo)) {
            log.warn("Operation log skipped: empty bizNo for method [{}#{}]",
                    targetClass.getSimpleName(), method.getName());
            return;
        }

        LogOperator operator = resolveOperator(operation, context, key);

        String requestIp = null;
        String userAgent = null;
        String requestParams = null;
        LogRecordProperties.Request request = this.properties.getRequest();
        if (request != null) {
            if (request.isEnabled()) {
                requestIp = this.requestResolver.getClientIp();
                userAgent = this.requestResolver.getUserAgent();
            }
            if (request.isParamsEnabled()) {
                requestParams = LogRecordTextUtils.truncate(
                        this.parameterSerializer.serialize(method, args), request.getParamsMaxLength());
            }
        }

        content = LogRecordTextUtils.truncate(content, this.properties.getContentMaxLength());
        detail = LogRecordTextUtils.truncate(detail, this.properties.getDetailMaxLength());
        errorMsg = LogRecordTextUtils.truncate(errorMsg, this.properties.getErrorMaxLength());

        OperationLog operationLog = OperationLog.builder()
                .type(operation.type())
                .category(operation.category())
                .bizNo(bizNo)
                .success(success)
                .content(content)
                .detail(detail)
                .operatorId(operator == null ? null : operator.id())
                .operatorName(operator == null ? null : operator.name())
                .errorMsg(errorMsg)
                .method(targetClass.getSimpleName() + "#" + method.getName())
                .requestIp(requestIp)
                .userAgent(userAgent)
                .requestParams(requestParams)
                .createdAt(LocalDateTime.now())
                .build();

        this.store.save(operationLog);
    }

    private LogOperator resolveOperator(LogRecordOperation operation, EvaluationContext context,
            AnnotatedElementKey key) {
        if (StringUtils.hasText(operation.operator())) {
            Object value = this.valueParser.parseSingleExpression(operation.operator(), context, key);
            if (value instanceof LogOperator logOperator) {
                return logOperator;
            }
            if (value != null) {
                String id = String.valueOf(value);
                LogOperator provider = resolveProvider();
                String name = provider != null && id.equals(provider.id()) ? provider.name() : "";
                return new LogOperator(id, name);
            }
        }
        return resolveProvider();
    }

    private LogOperator resolveProvider() {
        try {
            return this.operatorProvider.getOperator();
        } catch (Exception e) {
            log.warn("Failed to resolve operator via provider [{}], falling back to default",
                    this.operatorProvider.getClass().getName(), e);
            return new DefaultLogOperatorProvider(this.properties).getOperator();
        }
    }
}
