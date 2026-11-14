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
 * <p>解析顺序：{@code @FlywayDataSource} → {@link FlywayConnectionDetails}（缺少时改用
 * {@link JdbcConnectionDetails}）→ {@code spring.flyway.url/user/password/driver-class-name}
 * → 唯一或 {@code @Primary} 的 {@link DataSource}。</p>
 *
 * @author Itsuka
 */
@FunctionalInterface
interface ItsukaFlywayDataSourceResolver {

    /**
     * 解析迁移数据源。
     *
     * @param sourceName 当前 source 名，仅用于错误信息
     * @return 迁移使用的数据源
     * @throws IllegalStateException 未找到任何可用的数据源时抛出
     */
    DataSource resolve(String sourceName);

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
        return (sourceName) -> {
            DataSource annotated = flywayDataSource.getIfAvailable();
            if (annotated != null) {
                return annotated;
            }

            FlywayConnectionDetails details = flywayConnectionDetails.getIfAvailable();
            if (details == null) {
                details = adapt(jdbcConnectionDetails.getIfAvailable());
            }
            DataSource uniqueDataSource = dataSource.getIfUnique();
            if (details != null) {
                DataSource resolved = resolveConnectionDetails(details, uniqueDataSource);
                if (resolved != null) {
                    return resolved;
                }
            }

            String url = properties.getUrl();
            if (StringUtils.hasText(url)) {
                return buildDataSource(url, properties.getUser(), properties.getPassword(),
                    properties.getDriverClassName());
            }
            if (StringUtils.hasText(properties.getUser()) && uniqueDataSource != null) {
                return deriveDataSource(uniqueDataSource, properties.getUser(), properties.getPassword());
            }
            if (uniqueDataSource != null) {
                return uniqueDataSource;
            }
            throw new IllegalStateException("Flyway 迁移数据源缺失（source: " + sourceName
                + "）：请提供一个 DataSource Bean、@FlywayDataSource Bean、FlywayConnectionDetails Bean、"
                + "JdbcConnectionDetails Bean，或配置 spring.flyway.url。");
        };
    }

    private static FlywayConnectionDetails adapt(JdbcConnectionDetails jdbcConnectionDetails) {
        if (jdbcConnectionDetails == null) {
            return null;
        }
        return new FlywayConnectionDetails() {
            @Override
            public String getUsername() {
                return jdbcConnectionDetails.getUsername();
            }

            @Override
            public String getPassword() {
                return jdbcConnectionDetails.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return jdbcConnectionDetails.getJdbcUrl();
            }
        };
    }

    private static DataSource resolveConnectionDetails(FlywayConnectionDetails details, DataSource uniqueDataSource) {
        if (StringUtils.hasText(details.getJdbcUrl())) {
            return buildDataSource(details.getJdbcUrl(), details.getUsername(), details.getPassword(),
                details.getDriverClassName());
        }
        if (StringUtils.hasText(details.getUsername()) && uniqueDataSource != null) {
            return deriveDataSource(uniqueDataSource, details.getUsername(), details.getPassword());
        }
        return null;
    }

    private static SimpleDriverDataSource buildDataSource(String url, String username, String password,
            String driverClassName) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
            .type(SimpleDriverDataSource.class)
            .url(url);
        if (username != null) {
            builder.username(username);
        }
        if (password != null) {
            builder.password(password);
        }
        if (StringUtils.hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        return (SimpleDriverDataSource) builder.build();
    }

    private static SimpleDriverDataSource deriveDataSource(DataSource source, String username, String password) {
        DataSourceBuilder<?> builder = DataSourceBuilder.derivedFrom(source)
            .type(SimpleDriverDataSource.class)
            .username(username);
        if (password != null) {
            builder.password(password);
        }
        return (SimpleDriverDataSource) builder.build();
    }
}
