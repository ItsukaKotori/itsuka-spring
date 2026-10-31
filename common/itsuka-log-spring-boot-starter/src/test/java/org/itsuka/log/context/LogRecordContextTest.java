package org.itsuka.log.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogRecordContextTest {

    @AfterEach
    void tearDown() {
        LogRecordContext.clear();
    }

    @Test
    void putsAndReadsVariable() {
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("oldAddress", "A");
        assertThat(LogRecordContext.getVariables())
            .containsEntry("oldAddress", "A");
    }

    @Test
    void nestedSpansDoNotOverwriteEachOther() {
        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("value", 1);

        LogRecordContext.pushSpan();
        LogRecordContext.putVariable("value", 2);
        assertThat(LogRecordContext.getVariables()).containsEntry("value", 2);
        LogRecordContext.popSpan();

        assertThat(LogRecordContext.getVariables()).containsEntry("value", 1);
        LogRecordContext.popSpan();
        assertThat(LogRecordContext.getVariables()).isEmpty();
    }

    @Test
    void popWithoutPushIsSafe() {
        LogRecordContext.popSpan();
        assertThat(LogRecordContext.getVariables()).isEmpty();
    }
}
