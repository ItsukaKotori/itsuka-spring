package org.itsuka.log.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcLogRecordStoreTest {

    private JdbcTemplate jdbc;
    private JdbcLogRecordStore store;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:logstore;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        props.setSchemaLocation("classpath:schema-h2.sql");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        store = new JdbcLogRecordStore(jdbc, props, null);
    }

    @Test
    void savesAndTruncatesLongValues() {
        OperationLog log = OperationLog.builder()
            .bizNo("B".repeat(200))
            .type("T".repeat(100))
            .success(true)
            .content("C".repeat(3000))
            .detail("D".repeat(5000))
            .operatorId("O".repeat(100))
            .operatorName("N".repeat(200))
            .method("M".repeat(300))
            .requestIp("I".repeat(100))
            .userAgent("U".repeat(600))
            .requestParams("P".repeat(5000))
            .createdAt(LocalDateTime.now())
            .build();

        store.save(log);

        Integer count = jdbc.queryForObject("select count(*) from itsuka_operation_log", Integer.class);
        assertThat(count).isEqualTo(1);
        String bizNo = jdbc.queryForObject("select biz_no from itsuka_operation_log", String.class);
        assertThat(bizNo).hasSize(128);
    }

    @Test
    void autoInitCanBeDisabled() {
        LogRecordProperties props = new LogRecordProperties();
        props.setAutoInitSchema(false);
        props.setSchemaLocation("classpath:schema-h2.sql");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:nodll;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        Integer count = new JdbcTemplate(ds)
            .queryForObject("select count(*) from information_schema.tables where table_name = 'itsuka_operation_log'", Integer.class);
        assertThat(count).isZero();
    }
}
