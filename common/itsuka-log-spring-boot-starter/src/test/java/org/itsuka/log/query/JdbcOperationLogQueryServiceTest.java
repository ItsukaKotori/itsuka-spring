package org.itsuka.log.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcOperationLogQueryServiceTest {

    private JdbcLogRecordStore store;
    private JdbcOperationLogQueryService queryService;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:logquery;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP ALL OBJECTS");
        LogRecordProperties props = new LogRecordProperties();
        props.setSchemaLocation("classpath:schema-h2.sql");
        props.getQuery().setDefaultPageSize(2);
        props.getQuery().setMaxPageSize(5);
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();
        store = new JdbcLogRecordStore(jdbc, props, null);
        queryService = new JdbcOperationLogQueryService(new NamedParameterJdbcTemplate(ds), props);

        store.save(log("NO1", "ORDER", true, LocalDateTime.now().minusMinutes(3)));
        store.save(log("NO1", "ORDER", false, LocalDateTime.now().minusMinutes(2)));
        store.save(log("NO2", "USER", true, LocalDateTime.now().minusMinutes(1)));
    }

    private OperationLog log(String bizNo, String type, boolean success, LocalDateTime time) {
        return OperationLog.builder().bizNo(bizNo).type(type).success(success)
            .content("content").operatorId("u1").createdAt(time).build();
    }

    @Test
    void pagesByBizNoWithDefaultSize() {
        OperationLogQuery query = new OperationLogQuery();
        query.setBizNo("NO1");
        OperationLogPage page = queryService.page(query);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void clampsOversizedPage() {
        OperationLogQuery query = new OperationLogQuery();
        query.setSize(100L);
        assertThat(queryService.page(query).getSize()).isEqualTo(5);
    }

    @Test
    void nullCurrentAndSizeUseDefaults() {
        OperationLogQuery query = new OperationLogQuery();
        query.setCurrent(null);
        query.setSize(null);
        OperationLogPage page = queryService.page(query);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void findsById() {
        OperationLogQuery query = new OperationLogQuery();
        query.setBizNo("NO2");
        Long id = queryService.page(query).getRecords().get(0).getId();
        assertThat(queryService.findById(id)).isPresent();
    }

    @Test
    void pageOffsetOverflowReturnsEmptyPage() {
        OperationLogQuery query = new OperationLogQuery();
        query.setCurrent(Long.MAX_VALUE);
        OperationLogPage page = queryService.page(query);
        assertThat(page.getRecords()).isEmpty();
        assertThat(page.getCurrent()).isEqualTo(Long.MAX_VALUE);
        assertThat(page.getTotal()).isEqualTo(3);
    }

    @Test
    void queriesReservedTableName() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:orderquery;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        props.setTableName("order");
        props.setSchemaLocation("classpath:schema-h2.sql");
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();

        JdbcLogRecordStore orderStore = new JdbcLogRecordStore(jdbc, props, null);
        orderStore.save(log("NO1", "ORDER", true, LocalDateTime.now()));

        JdbcOperationLogQueryService orderQuery =
            new JdbcOperationLogQueryService(new NamedParameterJdbcTemplate(ds), props);
        OperationLogQuery query = new OperationLogQuery();
        query.setBizNo("NO1");
        OperationLogPage page = orderQuery.page(query);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getBizNo()).isEqualTo("NO1");
    }
}
