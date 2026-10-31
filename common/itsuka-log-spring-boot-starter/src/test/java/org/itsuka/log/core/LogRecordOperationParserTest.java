package org.itsuka.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.itsuka.log.annotation.LogRecord;
import org.junit.jupiter.api.Test;

class LogRecordOperationParserTest {

    static class SampleService {
        @LogRecord(success = "ok", fail = "fail", operator = "{{#user}}",
            bizNo = "{{#id}}", type = "ORDER", category = "CUSTOMER",
            detail = "{{#detail}}", condition = "#id != null")
        public void annotated(String id) {}
        public void plain(String id) {}
    }

    private final LogRecordOperationParser parser = new LogRecordOperationParser();

    @Test
    void parsesAnnotation() throws Exception {
        Method method = SampleService.class.getMethod("annotated", String.class);
        LogRecordOperation op = parser.parse(method).orElseThrow();
        assertThat(op.success()).isEqualTo("ok");
        assertThat(op.fail()).isEqualTo("fail");
        assertThat(op.bizNo()).isEqualTo("{{#id}}");
        assertThat(op.type()).isEqualTo("ORDER");
        assertThat(op.condition()).isEqualTo("#id != null");
    }

    @Test
    void returnsEmptyWhenMissing() throws Exception {
        Method method = SampleService.class.getMethod("plain", String.class);
        assertThat(parser.parse(method)).isEmpty();
    }
}
