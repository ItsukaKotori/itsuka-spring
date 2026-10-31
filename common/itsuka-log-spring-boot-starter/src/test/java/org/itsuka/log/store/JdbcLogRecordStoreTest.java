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

    @Test
    void defensivelyCapsOverConfiguredLengths() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:capstore;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        LogRecordProperties initProps = new LogRecordProperties();
        initProps.setSchemaLocation("classpath:schema-h2.sql");
        new LogSchemaInitializer(ds, initProps, new DefaultResourceLoader()).afterPropertiesSet();

        LogRecordProperties props = new LogRecordProperties();
        props.setContentMaxLength(5000);
        props.setErrorMaxLength(5000);
        props.setDetailMaxLength(50000);
        props.getRequest().setParamsMaxLength(50000);
        JdbcLogRecordStore store = new JdbcLogRecordStore(jdbc, props, null);

        store.save(OperationLog.builder()
            .bizNo("B1").success(true)
            .content("C".repeat(3000))
            .errorMsg("E".repeat(3000))
            .createdAt(LocalDateTime.now())
            .build());

        String content = jdbc.queryForObject(
            "select content from itsuka_operation_log where biz_no = 'B1'", String.class);
        String errorMsg = jdbc.queryForObject(
            "select error_msg from itsuka_operation_log where biz_no = 'B1'", String.class);
        assertThat(content).hasSize(2000);
        assertThat(errorMsg).hasSize(2000);
    }

    @Test
    void quotesReservedTableNameInDml() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:orderstore;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        props.setTableName("order");
        props.setSchemaLocation("classpath:schema-h2.sql");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();

        JdbcLogRecordStore orderStore = new JdbcLogRecordStore(jdbc, props, null);
        orderStore.save(OperationLog.builder()
            .bizNo("B1").type("ORDER").success(true).content("x")
            .createdAt(LocalDateTime.now()).build());

        Integer count = jdbc.queryForObject("select count(*) from `order`", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
