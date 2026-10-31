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

    @Test
    void truncatesOnCodePointBoundaries() {
        assertThat(LogRecordTextUtils.truncate("a😀b", 4)).isEqualTo("a😀b");
        assertThat(LogRecordTextUtils.truncate("a😀b", 2)).isEqualTo("a😀");
        assertThat(LogRecordTextUtils.truncate("a😀b", 1)).isEqualTo("a");
    }
}
