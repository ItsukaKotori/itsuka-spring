package org.itsuka.log.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LogRecordPropertiesTest {

    @Test
    void defaultsAreUsable() {
        LogRecordProperties props = new LogRecordProperties();
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getStorage()).isEqualTo(LogRecordProperties.Storage.JDBC);
        assertThat(props.getTableName()).isEqualTo("itsuka_operation_log");
        assertThat(props.isAutoInitSchema()).isTrue();
        assertThat(props.getQuery().getDefaultPageSize()).isEqualTo(20);
        assertThat(props.getQuery().getMaxPageSize()).isEqualTo(200);
        assertThat(props.getAsync().getRejectionPolicy())
            .isEqualTo(LogRecordProperties.RejectionPolicy.CALLER_RUNS);
        props.validate();
    }

    @Test
    void rejectsInvalidTableName() {
        LogRecordProperties props = new LogRecordProperties();
        props.setTableName("bad-name;drop");
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tableName");

        LogRecordProperties leadingDigit = new LogRecordProperties();
        leadingDigit.setTableName("1table");
        assertThatThrownBy(leadingDigit::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tableName");
    }

    @Test
    void rejectsInvalidPoolSizes() {
        LogRecordProperties props = new LogRecordProperties();
        props.getAsync().setCorePoolSize(4);
        props.getAsync().setMaxPoolSize(2);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPoolSize");
    }
}
