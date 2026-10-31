package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.junit.jupiter.api.Test;

class DefaultLogOperatorProviderTest {

    @Test
    void returnsConfiguredDefaults() {
        LogRecordProperties props = new LogRecordProperties();
        props.getOperator().setDefaultId("system");
        props.getOperator().setDefaultName("系统");
        LogOperator operator = new DefaultLogOperatorProvider(props).getOperator();
        assertThat(operator.id()).isEqualTo("system");
        assertThat(operator.name()).isEqualTo("系统");
    }
}
