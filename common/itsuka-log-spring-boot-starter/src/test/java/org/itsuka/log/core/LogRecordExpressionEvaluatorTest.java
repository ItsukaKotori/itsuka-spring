package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.context.LogRecordContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

class LogRecordExpressionEvaluatorTest {

    record Address(String city) {}
    record Param(String orderNo, Address address) {}

    static class SampleService {
        public String update(Param param, int count) {
            return "NO1";
        }
    }

    private final LogRecordExpressionEvaluator evaluator = new LogRecordExpressionEvaluator();

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    private EvaluationContext context(Method method, Object result, String error) {
        return evaluator.createContext(
            method,
            new Object[]{new Param("NO1", new Address("上海")), 3},
            SampleService.class,
            result,
            error,
            null
        );
    }

    @Test
    void evaluatesParametersReturnAndError() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        EvaluationContext ctx = context(method, "OK", "boom");

        assertThat(evaluator.evaluate("#param.orderNo", key, ctx)).isEqualTo("NO1");
        assertThat(evaluator.evaluate("#param.address.city", key, ctx)).isEqualTo("上海");
        assertThat(evaluator.evaluate("#count > 2", key, ctx)).isEqualTo(true);
        assertThat(evaluator.evaluate("#_ret", key, ctx)).isEqualTo("OK");
        assertThat(evaluator.evaluate("#_errorMsg", key, ctx)).isEqualTo("boom");
    }

    @Test
    void evaluatesContextVariable() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("oldAddress", "北京");

        assertThat(evaluator.evaluate("#oldAddress", key, context(method, null, null)))
            .isEqualTo("北京");
    }

    @Test
    void fallsBackToPositionalNames() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class, int.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        EvaluationContext ctx = context(method, null, null);

        assertThat(evaluator.evaluate("#a0.orderNo", key, ctx)).isEqualTo("NO1");
        assertThat(evaluator.evaluate("#p1", key, ctx)).isEqualTo(3);
    }
}
