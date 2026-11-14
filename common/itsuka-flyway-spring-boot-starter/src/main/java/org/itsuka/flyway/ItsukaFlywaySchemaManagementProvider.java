package org.itsuka.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

/**
 * 根据已注册的 {@link Flyway} Bean 报告数据源的 schema 管理状态。
 *
 * @author Itsuka
 */
final class ItsukaFlywaySchemaManagementProvider implements SchemaManagementProvider {

    private final ObjectProvider<Flyway> flyways;

    ItsukaFlywaySchemaManagementProvider(ObjectProvider<Flyway> flyways) {
        this.flyways = flyways;
    }

    @Override
    public SchemaManagement getSchemaManagement(DataSource dataSource) {
        boolean managed = this.flyways.orderedStream()
            .map(flyway -> flyway.getConfiguration().getDataSource())
            .anyMatch(dataSource::equals);
        return managed ? SchemaManagement.MANAGED : SchemaManagement.UNMANAGED;
    }
}
