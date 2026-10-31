package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class Slf4jLogRecordStoreTest {

    @Test
    void writesToDedicatedLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger("itsuka.operation.log");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        new Slf4jLogRecordStore().save(OperationLog.builder()
            .bizNo("NO1").success(true).content("订单创建").createdAt(LocalDateTime.now()).build());

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("NO1", "订单创建");
        logger.detachAppender(appender);
    }
}
