package org.itsuka.log.query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.itsuka.log.domain.OperationLog;
import org.itsuka.log.domain.OperationLogPage;
import org.itsuka.log.domain.OperationLogQuery;
import org.itsuka.log.properties.LogRecordProperties;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * {@link OperationLogQueryService} backed by a {@link NamedParameterJdbcTemplate}.
 *
 * <p>All filter conditions are optional and bound as named parameters; only the validated
 * table name is spliced into the SQL. Page size is clamped to {@code query.maxPageSize} and
 * defaults to {@code query.defaultPageSize}.</p>
 */
public class JdbcOperationLogQueryService implements OperationLogQueryService {

    private static final String COLUMNS = """
            id, type, category, biz_no, success, content, detail, operator_id, operator_name,
            error_msg, method, request_ip, user_agent, request_params, created_at
            """;

    private static final RowMapper<OperationLog> ROW_MAPPER = (rs, rowNum) -> OperationLog.builder()
            .id(rs.getLong("id"))
            .type(rs.getString("type"))
            .category(rs.getString("category"))
            .bizNo(rs.getString("biz_no"))
            .success(rs.getBoolean("success"))
            .content(rs.getString("content"))
            .detail(rs.getString("detail"))
            .operatorId(rs.getString("operator_id"))
            .operatorName(rs.getString("operator_name"))
            .errorMsg(rs.getString("error_msg"))
            .method(rs.getString("method"))
            .requestIp(rs.getString("request_ip"))
            .userAgent(rs.getString("user_agent"))
            .requestParams(rs.getString("request_params"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .build();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final LogRecordProperties properties;

    /**
     * Creates a query service backed by the given JDBC template and configuration.
     *
     * @param jdbcTemplate the JDBC template used to execute the queries
     * @param properties   the operation log properties
     */
    public JdbcOperationLogQueryService(NamedParameterJdbcTemplate jdbcTemplate,
            LogRecordProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.properties.validate();
    }

    @Override
    public OperationLogPage page(OperationLogQuery query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = resolveSize(query.getSize());

        Where where = buildWhere(query);
        String from = " from " + tableName() + (where.sql().isEmpty() ? "" : " where " + where.sql());
        MapSqlParameterSource params = where.params();

        Long total = this.jdbcTemplate.queryForObject("select count(*) " + from, params, Long.class);

        String sql = "select " + COLUMNS + " " + from
                + " order by created_at desc, id desc limit :limit offset :offset";
        params.addValue("limit", size);
        params.addValue("offset", (current - 1) * size);
        List<OperationLog> records = this.jdbcTemplate.query(sql, params, ROW_MAPPER);

        return OperationLogPage.of(records, current, size, total == null ? 0 : total);
    }

    @Override
    public Optional<OperationLog> findById(Long id) {
        String sql = "select " + COLUMNS + " from " + tableName() + " where id = :id";
        List<OperationLog> result = this.jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id), ROW_MAPPER);
        return result.stream().findFirst();
    }

    private long resolveSize(Long requested) {
        long size = requested == null ? this.properties.getQuery().getDefaultPageSize() : requested;
        size = Math.min(size, this.properties.getQuery().getMaxPageSize());
        return Math.max(size, 1);
    }

    private Where buildWhere(OperationLogQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        if (hasText(query.getType())) {
            conditions.add("type = :type");
            params.addValue("type", query.getType());
        }
        if (hasText(query.getCategory())) {
            conditions.add("category = :category");
            params.addValue("category", query.getCategory());
        }
        if (hasText(query.getBizNo())) {
            conditions.add("biz_no = :bizNo");
            params.addValue("bizNo", query.getBizNo());
        }
        if (hasText(query.getOperatorId())) {
            conditions.add("operator_id = :operatorId");
            params.addValue("operatorId", query.getOperatorId());
        }
        if (query.getSuccess() != null) {
            conditions.add("success = :success");
            params.addValue("success", query.getSuccess());
        }
        if (query.getStartTime() != null) {
            conditions.add("created_at >= :startTime");
            params.addValue("startTime", query.getStartTime());
        }
        if (query.getEndTime() != null) {
            conditions.add("created_at <= :endTime");
            params.addValue("endTime", query.getEndTime());
        }
        return new Where(String.join(" and ", conditions), params);
    }

    private String tableName() {
        return this.properties.getTableName();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Immutable pair of a WHERE clause fragment and its bound named parameters.
     */
    private record Where(String sql, MapSqlParameterSource params) {
    }
}
