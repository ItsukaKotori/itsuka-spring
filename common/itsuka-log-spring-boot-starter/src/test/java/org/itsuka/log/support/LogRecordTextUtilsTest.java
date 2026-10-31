package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRecordTextUtilsTest {

    @Test
    void truncatesWithEllipsis() {
        assertThat(LogRecordTextUtils.truncate("abcdef", 4)).isEqualTo("a...");
        assertThat(LogRecordTextUtils.truncate("abc", 4)).isEqualTo("abc");
        assertThat(LogRecordTextUtils.truncate(null, 4)).isNull();
    }
}
