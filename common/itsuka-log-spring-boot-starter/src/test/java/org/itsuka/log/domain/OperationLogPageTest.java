package org.itsuka.log.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OperationLogPageTest {

    @Test
    void computesPages() {
        OperationLogPage page = OperationLogPage.of(List.of(), 1, 20, 41);
        assertThat(page.getPages()).isEqualTo(3);
        assertThat(page.getTotal()).isEqualTo(41);
    }

    @Test
    void zeroSizeHasZeroPages() {
        assertThat(OperationLogPage.of(List.of(), 1, 0, 10).getPages()).isZero();
    }
}
