package org.itsuka.flyway;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;

/**
 * 解析 Flyway 迁移使用的数据源。
 *
 * <p>解析顺序：{@code @FlywayDataSource} → {@link FlywayConnectionDetails}（含
 * {@code spring.flyway.*} 配置）→ 主 {@link DataSource} → {@link JdbcConnectionDetails}。</p>
 *
 * @author Itsuka
 */
@FunctionalInterface
interface ItsukaFlywayDataSourceResolver {

    /**
     * 解析迁移数据源。
     *
     * @return 迁移使用的数据源
     * @throws IllegalStateException 未找到任何可用的数据源时抛出
     */
    DataSource resolve();

    /**
     * 创建按固定优先级解析数据源的实例。
     *
     * @param properties 原生 Flyway 配置，用于回退 URL、用户名、密码与驱动类名
     * @param dataSource 主数据源
     * @param flywayDataSource {@code @FlywayDataSource} 标注的数据源
     * @param flywayConnectionDetails Flyway 连接详情
     * @param jdbcConnectionDetails JDBC 连接详情
     * @return 数据源解析器
     */
    static ItsukaFlywayDataSourceResolver of(FlywayProperties properties, ObjectProvider<DataSource> dataSource,
            ObjectProvider<DataSource> flywayDataSource,
            ObjectProvider<FlywayConnectionDetails> flywayConnectionDetails,
            ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetails) {
        return () -> {
            DataSource annotated = flywayDataSource.getIfAvailable();
            if (annotated != null) {
                return annotated;
            }
            FlywayConnectionDetails details = flywayConnectionDetails.getIfAvailable();
            if (details == null) {
                details = new FlywayConnectionDetails() {
                    @Override
                    public String getUsername() {
                        return properties.getUser();
                    }

                    @Override
                    public String getPassword() {
                        return properties.getPassword();
                    }

                    @Override
                    public String getJdbcUrl() {
                        return properties.getUrl();
                    }

                    @Override
                    public String getDriverClassName() {
                        return properties.getDriverClassName();
                    }
                };
            }
            String url = details.getJdbcUrl();
            if (url != null) {
                return buildDataSource(details,
                    DataSourceBuilder.create().type(SimpleDriverDataSource.class).url(url));
            }
            DataSource uniqueDataSource = dataSource.getIfUnique();
            if (details.getUsername() != null && uniqueDataSource != null) {
                return buildDataSource(details,
                    DataSourceBuilder.derivedFrom(uniqueDataSource).type(SimpleDriverDataSource.class));
            }
            if (uniqueDataSource != null) {
                return uniqueDataSource;
            }
            JdbcConnectionDetails jdbcDetails = jdbcConnectionDetails.getIfAvailable();
            if (jdbcDetails != null) {
                return DataSourceBuilder.create()
                    .type(SimpleDriverDataSource.class)
                    .url(jdbcDetails.getJdbcUrl())
                    .username(jdbcDetails.getUsername())
                    .password(jdbcDetails.getPassword())
                    .build();
            }
            throw new IllegalStateException(
                "Flyway 迁移数据源缺失：请提供一个 DataSource Bean、@FlywayDataSource Bean，或配置 spring.flyway.url。");
        };
    }

    private static DataSource buildDataSource(FlywayConnectionDetails details, DataSourceBuilder<?> builder) {
        builder.username(details.getUsername());
        builder.password(details.getPassword());
        String driverClassName = details.getDriverClassName();
        if (StringUtils.hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        return builder.build();
    }
}
