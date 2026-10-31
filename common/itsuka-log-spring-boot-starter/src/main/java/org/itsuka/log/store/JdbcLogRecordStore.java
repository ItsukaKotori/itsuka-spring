package org.itsuka.log.store;

import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRecordStore;
import org.itsuka.log.support.LogRecordTextUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link LogRecordStore} persisting operation logs through a {@link JdbcTemplate}.
 *
 * <p>Every string column is truncated to its column width (or the configured limit for
 * {@code content}/{@code detail}/{@code error}/{@code requestParams}) before insert, so an
 * overlong value never loses the whole record. Exceptions are never swallowed here; the aspect
 * is responsible for isolating storage failures from the business result.</p>
 */
public class JdbcLogRecordStore implements LogRecordStore {

    private static final String INSERT_SQL = """
            insert into %s
                (type, category, biz_no, success, content, detail, operator_id, operator_name,
                 error_msg, method, request_ip, user_agent, request_params, created_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    private final LogRecordProperties properties;

    @Nullable
    private final TransactionTemplate transactionTemplate;

    /**
     * Creates a store backed by the given JDBC template and configuration.
     *
     * @param jdbcTemplate       the JDBC template used to execute the insert
     * @param properties         the operation log properties
     * @param transactionTemplate optional transaction template; when present the insert runs
     *                            inside its transaction, otherwise directly on the template
     */
    public JdbcLogRecordStore(JdbcTemplate jdbcTemplate, LogRecordProperties properties,
            @Nullable TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void save(OperationLog log) {
        String sql = INSERT_SQL.formatted(this.properties.getTableName());
        if (this.transactionTemplate != null) {
            this.transactionTemplate.executeWithoutResult(status ->
                    this.jdbcTemplate.update(sql, args(log)));
        } else {
            this.jdbcTemplate.update(sql, args(log));
        }
    }

    private Object[] args(OperationLog log) {
        return new Object[] {
                LogRecordTextUtils.truncate(log.getType(), 64),
                LogRecordTextUtils.truncate(log.getCategory(), 64),
                LogRecordTextUtils.truncate(log.getBizNo(), 128),
                log.isSuccess(),
                LogRecordTextUtils.truncate(log.getContent(), this.properties.getContentMaxLength()),
                LogRecordTextUtils.truncate(log.getDetail(), this.properties.getDetailMaxLength()),
                LogRecordTextUtils.truncate(log.getOperatorId(), 64),
                LogRecordTextUtils.truncate(log.getOperatorName(), 128),
                LogRecordTextUtils.truncate(log.getErrorMsg(), this.properties.getErrorMaxLength()),
                LogRecordTextUtils.truncate(log.getMethod(), 255),
                LogRecordTextUtils.truncate(log.getRequestIp(), 64),
                LogRecordTextUtils.truncate(log.getUserAgent(), 512),
                LogRecordTextUtils.truncate(log.getRequestParams(),
                        this.properties.getRequest().getParamsMaxLength()),
                log.getCreatedAt()
        };
    }
}
