package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.itsuka.flyway.ItsukaFlywayProperties.Source;
import org.junit.jupiter.api.Test;

class ItsukaFlywayPropertiesValidatorTest {

    private static ItsukaFlywayProperties properties(Source... sources) {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        properties.setSources(List.of(sources));
        return properties;
    }

    private static Source source(String name, String location, String table) {
        Source source = new Source();
        source.setName(name);
        source.setLocation(location);
        source.setTable(table);
        return source;
    }

    @Test
    void acceptsValidSources() {
        assertThatCode(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", null),
                       source("user", "classpath:flyway/user", "custom_user_history"))))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("", "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].name");
    }

    @Test
    void rejectsInvalidName() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("1order", "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].name");
    }

    @Test
    void rejectsDuplicateNormalizedNames() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order-center", "classpath:flyway/a", null),
                       source("order_center", "classpath:flyway/b", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("重复");
    }

    @Test
    void rejectsBlankLocation() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", " ", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[0].location");
    }

    @Test
    void rejectsInvalidExplicitTable() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", "bad-name"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("table");
    }

    @Test
    void rejectsTooLongDerivedTable() {
        String longName = "a".repeat(60);
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source(longName, "classpath:flyway/order", null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("64");
    }

    @Test
    void rejectsDuplicateTablesIgnoringCase() {
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(
            properties(source("order", "classpath:flyway/order", "same_history"),
                       source("user", "classpath:flyway/user", "SAME_HISTORY"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("历史表");
    }

    @Test
    void rejectsNullSourceElement() {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        properties.setSources(Arrays.asList(source("order", "classpath:flyway/order", null), null));
        assertThatThrownBy(() -> ItsukaFlywayPropertiesValidator.validate(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("itsuka.flyway.sources[1]");
    }

    @Test
    void treatsNullSourceListAsEmpty() {
        ItsukaFlywayProperties properties = new ItsukaFlywayProperties();
        properties.setSources(null);
        assertThatCode(() -> ItsukaFlywayPropertiesValidator.validate(properties))
            .doesNotThrowAnyException();
    }
}
