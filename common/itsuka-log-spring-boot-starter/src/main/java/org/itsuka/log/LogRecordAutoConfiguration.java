package org.itsuka.log;

import java.util.List;

import org.itsuka.log.core.LogRecordAspect;
import org.itsuka.log.core.LogRecordExpressionEvaluator;
import org.itsuka.log.core.LogRecordOperationParser;
import org.itsuka.log.core.LogRecordValueParser;
import org.itsuka.log.function.LogRecordFunction;
import org.itsuka.log.function.LogRecordFunctionFactory;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogOperatorProvider;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.spi.LogRequestResolver;
import org.itsuka.log.support.DefaultLogOperatorProvider;
import org.itsuka.log.support.LogParameterSerializer;
import org.itsuka.log.support.LogRecordAsyncExecutor;
import org.itsuka.log.support.NoopLogRequestResolver;
import org.itsuka.log.support.ToStringLogParameterSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Core operation log auto-configuration wiring the parser, evaluator, value parser, function
 * factory, default SPI implementations and the aspect.
 *
 * <p>Runs after the security and web auto-configurations so that their SPI overrides (operator
 * provider, request resolver, parameter serializer) are registered first and win the
 * {@code @ConditionalOnMissingBean} race.</p>
 */
@AutoConfiguration(after = {LogRecordSecurityAutoConfiguration.class, LogRecordWebAutoConfiguration.class})
@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordAutoConfiguration {

    /**
     * Validates the bound properties eagerly so a misconfigured application fails fast.
     */
    public LogRecordAutoConfiguration(LogRecordProperties properties) {
        properties.validate();
    }

    @Bean
    @ConditionalOnMissingBean
    LogRecordOperationParser logRecordOperationParser() {
        return new LogRecordOperationParser();
    }

    @Bean
    @ConditionalOnMissingBean
    LogRecordExpressionEvaluator logRecordExpressionEvaluator() {
        return new LogRecordExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    LogRecordFunctionFactory logRecordFunctionFactory(List<LogRecordFunction> functions) {
        return new LogRecordFunctionFactory(functions);
    }

    @Bean
    @ConditionalOnMissingBean
    LogRecordValueParser logRecordValueParser(LogRecordExpressionEvaluator evaluator,
            LogRecordFunctionFactory functionFactory) {
        return new LogRecordValueParser(evaluator, functionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    LogOperatorProvider defaultLogOperatorProvider(LogRecordProperties properties) {
        return new DefaultLogOperatorProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    LogRequestResolver noopLogRequestResolver() {
        return new NoopLogRequestResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    LogParameterSerializer toStringLogParameterSerializer() {
        return new ToStringLogParameterSerializer();
    }

    @Bean(name = "itsukaLogExecutor")
    @ConditionalOnMissingBean(name = "itsukaLogExecutor")
    @ConditionalOnProperty(prefix = "itsuka.log.async", name = "enabled", havingValue = "true")
    ThreadPoolTaskExecutor itsukaLogExecutor(LogRecordProperties properties) {
        return LogRecordAsyncExecutor.create(properties.getAsync());
    }

    @Bean
    LogRecordAspect logRecordAspect(LogRecordProperties properties,
            LogRecordOperationParser operationParser,
            LogRecordExpressionEvaluator expressionEvaluator,
            LogRecordValueParser valueParser,
            LogOperatorProvider operatorProvider,
            LogRequestResolver requestResolver,
            LogParameterSerializer parameterSerializer,
            ObjectProvider<LogRecordStore> storeProvider) {
        LogRecordStore store = LogRecordStorageValidator.require(storeProvider, properties);
        return new LogRecordAspect(properties, operationParser, expressionEvaluator, valueParser,
                operatorProvider, requestResolver, parameterSerializer, store);
    }
}
