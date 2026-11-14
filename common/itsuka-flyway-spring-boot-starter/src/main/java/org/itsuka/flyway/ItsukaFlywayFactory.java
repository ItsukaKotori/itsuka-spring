package org.itsuka.flyway;

import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 按单个 source 的配置创建并加载 {@link Flyway} 实例。
 *
 * <p>公共的 {@code spring.flyway.*} 配置先应用，随后依次应用 callbacks、JavaMigrations 与
 * 自定义配置；source 的 location 与历史表最后应用，确保不被自定义配置覆盖。</p>
 *
 * @author Itsuka
 */
final class ItsukaFlywayFactory {

    private static final String VENDOR_PLACEHOLDER = "{vendor}";

    private final FlywayProperties properties;

    private final ItsukaFlywayProperties starterProperties;

    private final ResourceLoader resourceLoader;

    private final List<FlywayConfigurationCustomizer> customizers;

    private final List<JavaMigration> javaMigrations;

    private final List<Callback> callbacks;

    private final ItsukaFlywayDataSourceResolver dataSourceResolver;

    ItsukaFlywayFactory(FlywayProperties properties, ItsukaFlywayProperties starterProperties,
            ResourceLoader resourceLoader, ItsukaFlywayDataSourceResolver dataSourceResolver,
            List<FlywayConfigurationCustomizer> customizers, List<JavaMigration> javaMigrations,
            List<Callback> callbacks) {
        this.properties = properties;
        this.starterProperties = starterProperties;
        this.resourceLoader = resourceLoader;
        this.customizers = customizers;
        this.javaMigrations = javaMigrations;
        this.callbacks = callbacks;
        this.dataSourceResolver = dataSourceResolver;
    }

    /**
     * 创建并加载指定 source 的 {@link Flyway}，不会执行迁移。
     *
     * @param source 迁移路径配置
     * @return 已加载配置的 Flyway 实例
     */
    Flyway create(ItsukaFlywayProperties.Source source) {
        DataSource dataSource = this.dataSourceResolver.resolve(source.getName());
        FluentConfiguration configuration = new FluentConfiguration(this.resourceLoader.getClassLoader());
        configuration.dataSource(dataSource);
        configureProperties(configuration, this.properties, dataSource);
        configureCallbacks(configuration);
        configureJavaMigrations(configuration);
        this.customizers.forEach((customizer) -> customizer.customize(configuration));
        configuration.locations(source.getLocation());
        configuration.table(source.resolvedTable());
        configuration.failOnMissingLocations(this.starterProperties.isFailOnMissingLocations());
        return configuration.load();
    }

    /**
     * 复制 Spring Boot 4.0.3 {@code FlywayAutoConfiguration} 的属性映射，但不映射
     * {@code locations} 与 {@code table}；映射改编自 Spring Boot，遵循 Apache License 2.0。
     *
     * @param configuration the configuration
     * @param properties the properties
     * @param dataSource 迁移数据源，用于解析 callback location 中的 {@code {vendor}}
     */
    @SuppressWarnings("removal")
    private void configureProperties(FluentConfiguration configuration, FlywayProperties properties,
            DataSource dataSource) {
        // NOTE: Using method references in the mapper methods can break
        // back-compatibility (see gh-38164)
        PropertyMapper map = PropertyMapper.get();
        map.from(properties.getCallbackLocations())
            .when((callbackLocations) -> !ObjectUtils.isEmpty(callbackLocations))
            .to((callbackLocations) -> configuration.callbackLocations(
                    resolveVendorLocations(callbackLocations, dataSource).toArray(new String[0])));
        map.from(properties.isFailOnMissingLocations())
            .to((failOnMissingLocations) -> configuration.failOnMissingLocations(failOnMissingLocations));
        map.from(properties.getEncoding()).to((encoding) -> configuration.encoding(encoding));
        map.from(properties.getConnectRetries())
            .to((connectRetries) -> configuration.connectRetries(connectRetries));
        map.from(properties.getConnectRetriesInterval())
            .as(Duration::getSeconds)
            .as(Long::intValue)
            .to((connectRetriesInterval) -> configuration.connectRetriesInterval(connectRetriesInterval));
        map.from(properties.getLockRetryCount())
            .to((lockRetryCount) -> configuration.lockRetryCount(lockRetryCount));
        map.from(properties.getDefaultSchema()).to((schema) -> configuration.defaultSchema(schema));
        map.from(properties.getSchemas())
            .as(StringUtils::toStringArray)
            .to((schemas) -> configuration.schemas(schemas));
        map.from(properties.isCreateSchemas()).to((createSchemas) -> configuration.createSchemas(createSchemas));
        map.from(properties.getTablespace()).to((tablespace) -> configuration.tablespace(tablespace));
        map.from(properties.getBaselineDescription())
            .to((baselineDescription) -> configuration.baselineDescription(baselineDescription));
        map.from(properties.getBaselineVersion())
            .to((baselineVersion) -> configuration.baselineVersion(baselineVersion));
        map.from(properties.getInstalledBy()).to((installedBy) -> configuration.installedBy(installedBy));
        map.from(properties.getPlaceholders()).to((placeholders) -> configuration.placeholders(placeholders));
        map.from(properties.getPlaceholderPrefix())
            .to((placeholderPrefix) -> configuration.placeholderPrefix(placeholderPrefix));
        map.from(properties.getPlaceholderSuffix())
            .to((placeholderSuffix) -> configuration.placeholderSuffix(placeholderSuffix));
        map.from(properties.getPlaceholderSeparator())
            .to((placeHolderSeparator) -> configuration.placeholderSeparator(placeHolderSeparator));
        map.from(properties.isPlaceholderReplacement())
            .to((placeholderReplacement) -> configuration.placeholderReplacement(placeholderReplacement));
        map.from(properties.getSqlMigrationPrefix())
            .to((sqlMigrationPrefix) -> configuration.sqlMigrationPrefix(sqlMigrationPrefix));
        map.from(properties.getSqlMigrationSuffixes())
            .as(StringUtils::toStringArray)
            .to((sqlMigrationSuffixes) -> configuration.sqlMigrationSuffixes(sqlMigrationSuffixes));
        map.from(properties.getSqlMigrationSeparator())
            .to((sqlMigrationSeparator) -> configuration.sqlMigrationSeparator(sqlMigrationSeparator));
        map.from(properties.getRepeatableSqlMigrationPrefix())
            .to((repeatableSqlMigrationPrefix) -> configuration
                .repeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix));
        map.from(properties.getTarget()).to((target) -> configuration.target(target));
        map.from(properties.isBaselineOnMigrate())
            .to((baselineOnMigrate) -> configuration.baselineOnMigrate(baselineOnMigrate));
        map.from(properties.isCleanDisabled()).to((cleanDisabled) -> configuration.cleanDisabled(cleanDisabled));
        map.from(properties.isGroup()).to((group) -> configuration.group(group));
        map.from(properties.isMixed()).to((mixed) -> configuration.mixed(mixed));
        map.from(properties.isOutOfOrder()).to((outOfOrder) -> configuration.outOfOrder(outOfOrder));
        map.from(properties.isSkipDefaultCallbacks())
            .to((skipDefaultCallbacks) -> configuration.skipDefaultCallbacks(skipDefaultCallbacks));
        map.from(properties.isSkipDefaultResolvers())
            .to((skipDefaultResolvers) -> configuration.skipDefaultResolvers(skipDefaultResolvers));
        map.from(properties.isValidateMigrationNaming())
            .to((validateMigrationNaming) -> configuration.validateMigrationNaming(validateMigrationNaming));
        map.from(properties.isValidateOnMigrate())
            .to((validateOnMigrate) -> configuration.validateOnMigrate(validateOnMigrate));
        map.from(properties.getInitSqls())
            .whenNot(CollectionUtils::isEmpty)
            .as((initSqls) -> StringUtils.collectionToDelimitedString(initSqls, "\n"))
            .to((initSql) -> configuration.initSql(initSql));
        map.from(properties.getScriptPlaceholderPrefix())
            .to((prefix) -> configuration.scriptPlaceholderPrefix(prefix));
        map.from(properties.getScriptPlaceholderSuffix())
            .to((suffix) -> configuration.scriptPlaceholderSuffix(suffix));
        map.from(properties.getPowershellExecutable())
            .to((powershellExecutable) -> configuration.powershellExecutable(powershellExecutable));
        configureExecuteInTransaction(configuration, properties, map);
        map.from(properties::getLoggers).to((loggers) -> configuration.loggers(loggers));
        map.from(properties::getCommunityDbSupportEnabled)
            .to((communityDbSupportEnabled) -> configuration.communityDBSupportEnabled(communityDbSupportEnabled));
        map.from(properties.getBatch()).to((batch) -> configuration.batch(batch));
        map.from(properties.getDryRunOutput()).to((dryRunOutput) -> configuration.dryRunOutput(dryRunOutput));
        map.from(properties.getErrorOverrides())
            .to((errorOverrides) -> configuration.errorOverrides(errorOverrides));
        map.from(properties.getStream()).to((stream) -> configuration.stream(stream));
        map.from(properties.getJdbcProperties())
            .whenNot(Map::isEmpty)
            .to((jdbcProperties) -> configuration.jdbcProperties(jdbcProperties));
        map.from(properties.getKerberosConfigFile())
            .to((configFile) -> configuration.kerberosConfigFile(configFile));
        map.from(properties.getOutputQueryResults())
            .to((outputQueryResults) -> configuration.outputQueryResults(outputQueryResults));
        map.from(properties.getSkipExecutingMigrations())
            .to((skipExecutingMigrations) -> configuration.skipExecutingMigrations(skipExecutingMigrations));
        map.from(properties.getIgnoreMigrationPatterns())
            .to((ignoreMigrationPatterns) -> configuration
                .ignoreMigrationPatterns(ignoreMigrationPatterns.toArray(new String[0])));
        map.from(properties.getDetectEncoding())
            .to((detectEncoding) -> configuration.detectEncoding(detectEncoding));
    }

    private void configureExecuteInTransaction(FluentConfiguration configuration, FlywayProperties properties,
            PropertyMapper map) {
        try {
            map.from(properties.isExecuteInTransaction()).to(configuration::executeInTransaction);
        }
        catch (NoSuchMethodError ex) {
            // Flyway < 9.14
        }
    }

    private void configureCallbacks(FluentConfiguration configuration) {
        if (!this.callbacks.isEmpty()) {
            configuration.callbacks(this.callbacks.toArray(Callback[]::new));
        }
    }

    private void configureJavaMigrations(FluentConfiguration configuration) {
        if (!this.javaMigrations.isEmpty()) {
            configuration.javaMigrations(this.javaMigrations.toArray(JavaMigration[]::new));
        }
    }

    private List<String> resolveVendorLocations(List<String> locations, DataSource dataSource) {
        if (!usesVendorPlaceholder(locations)) {
            return locations;
        }
        DatabaseDriver databaseDriver = getDatabaseDriver(dataSource);
        if (databaseDriver == DatabaseDriver.UNKNOWN) {
            return locations;
        }
        String vendor = databaseDriver.getId();
        return locations.stream().map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor)).toList();
    }

    private DatabaseDriver getDatabaseDriver(DataSource dataSource) {
        try {
            String url = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
            return DatabaseDriver.fromJdbcUrl(url);
        }
        catch (MetaDataAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private boolean usesVendorPlaceholder(List<String> locations) {
        for (String location : locations) {
            if (location.contains(VENDOR_PLACEHOLDER)) {
                return true;
            }
        }
        return false;
    }
}
