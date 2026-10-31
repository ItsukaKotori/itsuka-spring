package org.itsuka.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.query.JdbcOperationLogQueryService;
import org.itsuka.log.store.JdbcLogRecordStore;
import org.itsuka.log.store.LogSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MySqlSchemaIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    void createsSchemaSavesAndQueries() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        LogRecordProperties props = new LogRecordProperties();
        new LogSchemaInitializer(ds, props, new DefaultResourceLoader()).afterPropertiesSet();

        JdbcLogRecordStore store = new JdbcLogRecordStore(jdbc, props, null);
        store.save(OperationLog.builder()
            .bizNo("NO1").type("ORDER").success(true)
            .content("订单创建").createdAt(LocalDateTime.now()).build());

        JdbcOperationLogQueryService query = new JdbcOperationLogQueryService(
            new NamedParameterJdbcTemplate(ds), props);
        OperationLogQuery q = new OperationLogQuery();
        q.setBizNo("NO1");
        OperationLogPage page = query.page(q);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getContent()).isEqualTo("订单创建");
    }
}
