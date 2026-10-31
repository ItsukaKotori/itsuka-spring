package org.itsuka.log.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    void rejectsContentLengthOverColumnLimit() {
        LogRecordProperties props = new LogRecordProperties();
        props.setContentMaxLength(2001);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contentMaxLength");
    }

    @Test
    void rejectsErrorLengthOverColumnLimit() {
        LogRecordProperties props = new LogRecordProperties();
        props.setErrorMaxLength(2001);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("errorMaxLength");
    }

    @Test
    void rejectsDetailLengthOverColumnLimit() {
        LogRecordProperties props = new LogRecordProperties();
        props.setDetailMaxLength(16001);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("detailMaxLength");
    }

    @Test
    void rejectsRequestParamsLengthOverColumnLimit() {
        LogRecordProperties props = new LogRecordProperties();
        props.getRequest().setParamsMaxLength(16001);
        assertThatThrownBy(props::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("paramsMaxLength");
    }

    @Test
    void acceptsLengthsAtColumnLimits() {
        LogRecordProperties props = new LogRecordProperties();
        props.setContentMaxLength(2000);
        props.setErrorMaxLength(2000);
        props.setDetailMaxLength(16000);
        props.getRequest().setParamsMaxLength(16000);
        assertThatCode(props::validate).doesNotThrowAnyException();
    }
}
