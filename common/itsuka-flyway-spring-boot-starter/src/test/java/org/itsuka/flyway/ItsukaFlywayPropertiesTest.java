package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItsukaFlywayPropertiesTest {

    @Test
    void defaults() {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isNativeEnabled()).isFalse();
        assertThat(properties.isFailOnMissingLocations()).isTrue();
        assertThat(properties.getSources()).isEmpty();
    }

    @Test
    void derivesTableNameFromNormalizedName() {
        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order-center");
        assertThat(source.normalizedName()).isEqualTo("order_center");
        assertThat(source.resolvedTable()).isEqualTo("flyway_schema_history_order_center");
    }

    @Test
    void explicitTableWins() {
        ItsukaFlywayProperties.Source source = new ItsukaFlywayProperties.Source();
        source.setName("order");
        source.setTable("custom_history");
        assertThat(source.resolvedTable()).isEqualTo("custom_history");
    }

    @Test
    void nullSourcesAreNormalizedToEmptyList() {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        properties.setSources(null);
        assertThat(properties.getSources()).isEmpty();
    }
}
