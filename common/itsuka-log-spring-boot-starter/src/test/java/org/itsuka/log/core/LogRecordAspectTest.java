package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.itsuka.log.annotation.LogRecord;
import org.itsuka.log.context.LogRecordContext;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.support.DefaultLogOperatorProvider;
import org.itsuka.log.support.NoopLogRequestResolver;
import org.itsuka.log.support.ToStringLogParameterSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class LogRecordAspectTest {

    static class SampleService {
        @LogRecord(
            success = "订单{{#orderNo}}创建成功",
            fail = "订单{{#orderNo}}创建失败：{{#_errorMsg}}",
            bizNo = "{{#orderNo}}",
            type = "ORDER"
        )
        public String create(String orderNo) {
            return "OK";
        }

        @LogRecord(success = "不会记录", bizNo = "{{#orderNo}}", condition = "#orderNo == null")
        public void conditionFalse(String orderNo) {
        }

        @LogRecord(success = "空业务号", bizNo = "{{#missing}}")
        public void blankBizNo() {
        }
    }

    static class FailingService {
        @LogRecord(
            success = "订单{{#orderNo}}创建成功",
            fail = "订单{{#orderNo}}创建失败：{{#_errorMsg}}",
            bizNo = "{{#orderNo}}",
            type = "ORDER"
        )
        public String create(String orderNo) {
            throw new IllegalStateException("boom");
        }
    }

    private final List<OperationLog> saved = new ArrayList<>();

    private <T> T proxy(T target, LogRecordProperties props, LogRecordStore store) {
        LogRecordAspect aspect = new LogRecordAspect(
            props,
            new LogRecordOperationParser(),
            new LogRecordExpressionEvaluator(),
            new LogRecordValueParser(new LogRecordExpressionEvaluator(),
                new org.itsuka.log.function.LogRecordFunctionFactory(List.of())),
            new DefaultLogOperatorProvider(props),
            new NoopLogRequestResolver(),
            new ToStringLogParameterSerializer(),
            store
        );
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    @Test
    void recordsSuccess() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        assertThat(service.create("NO1")).isEqualTo("OK");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isSuccess()).isTrue();
        assertThat(saved.get(0).getBizNo()).isEqualTo("NO1");
        assertThat(saved.get(0).getContent()).isEqualTo("订单NO1创建成功");
        assertThat(saved.get(0).getType()).isEqualTo("ORDER");
    }

    @Test
    void recordsFailureAndRethrowsOriginalException() {
        FailingService failing = proxy(new FailingService(), new LogRecordProperties(), saved::add);

        assertThatThrownBy(() -> failing.create("NO1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isSuccess()).isFalse();
        assertThat(saved.get(0).getErrorMsg()).isEqualTo("boom");
        assertThat(saved.get(0).getContent()).isEqualTo("订单NO1创建失败：boom");
    }

    @Test
    void storageFailureDoesNotBreakBusiness() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), log -> {
            throw new IllegalStateException("db down");
        });
        assertThat(service.create("NO1")).isEqualTo("OK");
    }

    @Test
    void conditionFalseSkipsRecord() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        service.conditionFalse("NO1");
        assertThat(saved).isEmpty();
    }

    @Test
    void blankBizNoSkipsRecord() {
        SampleService service = proxy(new SampleService(), new LogRecordProperties(), saved::add);
        service.blankBizNo();
        assertThat(saved).isEmpty();
    }
}
