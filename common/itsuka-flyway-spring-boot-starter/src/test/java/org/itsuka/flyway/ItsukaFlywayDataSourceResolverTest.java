package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

class ItsukaFlywayDataSourceResolverTest {

    private final FlywayProperties properties = new FlywayProperties();

    @Test
    void annotatedDataSourceWins() {
        DataSource primary = dataSource("primary");
        DataSource flyway = dataSource("flyway");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), provider(flyway), emptyProvider(), emptyProvider());
        assertThat(resolver.resolve()).isSameAs(flyway);
    }

    @Test
    void flywayConnectionDetailsBuildsDataSourceFromUrl() {
        properties.setUrl("jdbc:h2:mem:flyway-details");
        properties.setUser("sa");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider());
        DataSource resolved = resolver.resolve();
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:flyway-details");
    }

    @Test
    void flywayConnectionDetailsBeanWinsOverPrimary() {
        DataSource primary = dataSource("primary");
        FlywayConnectionDetails details = new FlywayConnectionDetails() {
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
            @Override public String getJdbcUrl() { return "jdbc:h2:mem:details-bean"; }
        };
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), provider(details), emptyProvider());
        DataSource resolved = resolver.resolve();
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:details-bean");
    }

    @Test
    void jdbcConnectionDetailsIsFallback() {
        JdbcConnectionDetails details = new JdbcConnectionDetails() {
            @Override public String getUsername() { return "sa"; }
            @Override public String getPassword() { return ""; }
            @Override public String getJdbcUrl() { return "jdbc:h2:mem:jdbc-details"; }
        };
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), provider(details));
        DataSource resolved = resolver.resolve();
        assertThat(resolved).isInstanceOf(SimpleDriverDataSource.class);
        assertThat(((SimpleDriverDataSource) resolved).getUrl()).isEqualTo("jdbc:h2:mem:jdbc-details");
    }

    @Test
    void primaryDataSourceIsFallback() {
        DataSource primary = dataSource("primary");
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, provider(primary), emptyProvider(), emptyProvider(), emptyProvider());
        assertThat(resolver.resolve()).isSameAs(primary);
    }

    @Test
    void missingDataSourceFailsWithHint() {
        ItsukaFlywayDataSourceResolver resolver = ItsukaFlywayDataSourceResolver.of(
            properties, emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider());
        assertThatThrownBy(resolver::resolve)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("数据源");
    }

    private static JdbcDataSource dataSource(String name) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override public T getObject() { return value; }
            @Override public T getObject(Object... args) { return value; }
            @Override public Iterator<T> iterator() { return List.of(value).iterator(); }
        };
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override public T getObject() { throw new NoSuchBeanDefinitionException(Object.class); }
            @Override public T getObject(Object... args) { throw new NoSuchBeanDefinitionException(Object.class); }
            @Override public Iterator<T> iterator() { return Collections.emptyIterator(); }
        };
    }
}
