package org.itsuka.log.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogRecordFunctionFactoryTest {

    private LogRecordFunction fn(String name) {
        return new LogRecordFunction() {
            @Override public String functionName() { return name; }
            @Override public String apply(Object value) { return name + ":" + value; }
        };
    }

    @Test
    void findsFunctionByName() {
        LogRecordFunctionFactory factory = new LogRecordFunctionFactory(List.of(fn("ORDER")));
        assertThat(factory.get("ORDER").apply("1")).isEqualTo("ORDER:1");
        assertThat(factory.get("MISSING")).isNull();
    }

    @Test
    void duplicateNamesFailFast() {
        assertThatThrownBy(() -> new LogRecordFunctionFactory(List.of(fn("ORDER"), fn("ORDER"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ORDER");
    }
}
