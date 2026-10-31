package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.itsuka.log.annotation.LogRecord;
import org.itsuka.log.context.LogRecordContext;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.function.LogRecordFunction;
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

    static class BeforeFunctionFailingService {
        @LogRecord(success = "{BOOM{#orderNo}}", bizNo = "{{#orderNo}}")
        public String create(String orderNo) {
            return "OK";
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

    static class Holder {
        private String name;

        Holder(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class OperatorService {
        @LogRecord(
            operator = "{OLD{#holder.name}}",
            success = "操作人更新成功",
            bizNo = "{{#orderNo}}"
        )
        public String mutate(Holder holder, String orderNo) {
            holder.setName("new");
            return "OK";
        }
    }

    private final List<OperationLog> saved = new ArrayList<>();

    private <T> T proxy(T target, LogRecordProperties props, LogRecordStore store) {
        return proxy(target, props, store, List.of());
    }

    private <T> T proxy(T target, LogRecordProperties props, LogRecordStore store,
            List<LogRecordFunction> functions) {
        LogRecordAspect aspect = new LogRecordAspect(
            props,
            new LogRecordOperationParser(),
            new LogRecordExpressionEvaluator(),
            new LogRecordValueParser(new LogRecordExpressionEvaluator(),
                new org.itsuka.log.function.LogRecordFunctionFactory(functions)),
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

    @Test
    void beforeFunctionFailureDoesNotStopBusiness() {
        LogRecordFunction boom = new LogRecordFunction() {
            @Override
            public String functionName() {
                return "BOOM";
            }

            @Override
            public boolean executeBefore() {
                return true;
            }

            @Override
            public String apply(Object value) {
                throw new IllegalStateException("boom");
            }
        };
        BeforeFunctionFailingService service = proxy(
            new BeforeFunctionFailingService(), new LogRecordProperties(), saved::add, List.of(boom));
        assertThat(service.create("NO1")).isEqualTo("OK");
        assertThat(saved).isEmpty();
    }

    @Test
    void beforeFunctionValueIsUsedForOperator() {
        LogRecordFunction old = new LogRecordFunction() {
            @Override
            public String functionName() {
                return "OLD";
            }

            @Override
            public boolean executeBefore() {
                return true;
            }

            @Override
            public String apply(Object value) {
                return value == null ? null : value.toString();
            }
        };
        OperatorService service = proxy(
            new OperatorService(), new LogRecordProperties(), saved::add, List.of(old));
        Holder holder = new Holder("old");
        assertThat(service.mutate(holder, "NO1")).isEqualTo("OK");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getOperatorId()).isEqualTo("old");
    }
}
