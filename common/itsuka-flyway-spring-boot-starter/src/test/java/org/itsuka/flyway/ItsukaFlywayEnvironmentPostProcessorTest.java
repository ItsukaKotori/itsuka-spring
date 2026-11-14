package org.itsuka.flyway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ItsukaFlywayEnvironmentPostProcessorTest {

    private final ItsukaFlywayEnvironmentPostProcessor postProcessor =
        new ItsukaFlywayEnvironmentPostProcessor();

    private static MockEnvironment environment(String... properties) {
        MockEnvironment environment = new MockEnvironment();
        for (int i = 0; i < properties.length; i += 2) {
            environment.setProperty(properties[i], properties[i + 1]);
        }
        return environment;
    }

    @Test
    void disablesNativeWhenNoSourcesConfigured() {
        MockEnvironment environment = environment();
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(environment.getPropertySources().contains("itsukaFlywayDefaults")).isTrue();
    }

    @Test
    void disablesNativeWhenStarterDisabled() {
        MockEnvironment environment = environment("itsuka.flyway.enabled", "false");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
    }

    @Test
    void leavesNativeAloneWhenSourcesConfigured() {
        MockEnvironment environment = environment(
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isNull();
    }

    @Test
    void leavesNativeAloneInNativeMode() {
        MockEnvironment environment = environment("itsuka.flyway.native-enabled", "true");
        postProcessor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.flyway.enabled")).isNull();
    }

    @Test
    void rejectsNativeModeWithSources() {
        MockEnvironment environment = environment(
            "itsuka.flyway.native-enabled", "true",
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("二选一");
    }

    @Test
    void rejectsNativeModeWithSourcesEvenWhenStarterDisabled() {
        MockEnvironment environment = environment(
            "itsuka.flyway.enabled", "false",
            "itsuka.flyway.native-enabled", "true",
            "itsuka.flyway.sources[0].name", "order",
            "itsuka.flyway.sources[0].location", "classpath:flyway/order");
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("二选一");
    }
}
