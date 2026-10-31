package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.itsuka.log.function.LogRecordFunction;
import org.itsuka.log.function.LogRecordFunctionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

class LogRecordValueParserTest {

    record Request(String address) {}
    record Param(String orderNo, Request request, String user) {}

    static class SampleService {
        public String update(Param param) { return "OK"; }
    }

    private final LogRecordExpressionEvaluator evaluator = new LogRecordExpressionEvaluator();

    private EvaluationContext context(Method method, Object result, String error) {
        return evaluator.createContext(
            method,
            new Object[]{new Param("NO1", new Request("A"), "张三")},
            SampleService.class, result, error, null
        );
    }

    private LogRecordValueParser parser(List<LogRecordFunction> functions) {
        return new LogRecordValueParser(evaluator, new LogRecordFunctionFactory(functions));
    }

    @Test
    void rendersPlainExpressions() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordValueParser parser = parser(List.of());

        assertThat(parser.parse("订单{{#param.orderNo}} 地址{{#param.request.address}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("订单NO1 地址A");
    }

    @Test
    void appliesFunctionAfterAndBefore() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        LogRecordFunction upper = new LogRecordFunction() {
            @Override public String functionName() { return "UPPER"; }
            @Override public String apply(Object value) { return value.toString().toUpperCase(); }
        };
        LogRecordFunction old = new LogRecordFunction() {
            @Override public String functionName() { return "OLD"; }
            @Override public boolean executeBefore() { return true; }
            @Override public String apply(Object value) { return "OLD:" + value; }
        };
        LogRecordValueParser parser = parser(List.of(upper, old));
        EvaluationContext ctx = evaluator.createContext(
            method,
            new Object[]{new Param("no1", new Request("A"), "张三")},
            SampleService.class, null, null, null);

        assertThat(parser.parse("{UPPER{{#param.orderNo}}}", ctx, key, Map.of()))
            .isEqualTo("NO1");
        assertThat(parser.parse("{OLD{{#param.orderNo}}}", ctx, key, Map.of("{OLD{{#param.orderNo}}}", "旧值")))
            .isEqualTo("旧值");

        Map<String, String> before = parser.parseBeforeFunctions(
            List.of("{OLD{{#param.orderNo}}}"), ctx, key);
        assertThat(before).containsEntry("{OLD{{#param.orderNo}}}", "OLD:no1");
    }

    @Test
    void nullExpressionRendersEmpty() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).parse("值={{#missing}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("值=");
    }

    @Test
    void unknownFunctionReturnsValue() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).parse("{MISSING{{#param.orderNo}}}",
            context(method, null, null), key, Map.of()))
            .isEqualTo("NO1");
    }

    @Test
    void evaluatesCondition() throws Exception {
        Method method = SampleService.class.getMethod("update", Param.class);
        AnnotatedElementKey key = new AnnotatedElementKey(method, SampleService.class);
        assertThat(parser(List.of()).evaluateCondition("#param.orderNo != null",
            context(method, null, null), key)).isTrue();
    }
}
