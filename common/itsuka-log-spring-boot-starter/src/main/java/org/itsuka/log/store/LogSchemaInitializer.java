package org.itsuka.log.store;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.itsuka.log.properties.LogRecordProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Idempotently creates the operation log table from the configured schema script.
 *
 * <p>The script may contain a {@code ${tableName}} placeholder which is replaced with the
 * configured table name. When {@code itsuka.log.auto-init-schema} is {@code false} the
 * initializer does nothing and the integrator is responsible for the schema.</p>
 */
public class LogSchemaInitializer implements InitializingBean {

    private static final String TABLE_NAME_PLACEHOLDER = "${tableName}";

    private final DataSource dataSource;

    private final LogRecordProperties properties;

    private final ResourceLoader resourceLoader;

    /**
     * Creates an initializer bound to the given datasource and configuration.
     *
     * @param dataSource     the datasource the schema is executed against
     * @param properties     the operation log properties
     * @param resourceLoader the loader used to resolve the schema location
     */
    public LogSchemaInitializer(DataSource dataSource, LogRecordProperties properties,
            ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void afterPropertiesSet() {
        this.properties.validate();
        if (!this.properties.isAutoInitSchema()) {
            return;
        }
        try {
            Resource resource = this.resourceLoader.getResource(this.properties.getSchemaLocation());
            String script;
            try (InputStream in = resource.getInputStream()) {
                script = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                        .replace(TABLE_NAME_PLACEHOLDER, this.properties.getTableName());
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)));
            populator.setContinueOnError(false);
            populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            DatabasePopulatorUtils.execute(populator, this.dataSource);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize operation log schema from ["
                            + this.properties.getSchemaLocation() + "]", e);
        }
    }
}
