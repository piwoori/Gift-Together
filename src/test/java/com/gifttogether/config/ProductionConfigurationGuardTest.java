package com.gifttogether.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationGuardTest {

    private final ProductionConfigurationGuard guard = new ProductionConfigurationGuard();

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.url", "spring.datasource.username", "spring.datasource.password", "jwt.secret"})
    void requiresEffectiveProductionSettings(String missing) {
        MockEnvironment complete = production();
        MockEnvironment incomplete = new MockEnvironment();
        incomplete.setActiveProfiles("prod");
        for (String key : new String[]{"spring.datasource.url", "spring.datasource.username", "spring.datasource.password", "jwt.secret"}) {
            if (!key.equals(missing)) {
                incomplete.setProperty(key, complete.getRequiredProperty(key));
            }
        }

        assertThatThrownBy(() -> guard.postProcessEnvironment(incomplete, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(missing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.password", "spring.flyway.password", "jwt.secret"})
    void rejectsBlankProductionSettings(String key) {
        assertThatThrownBy(() -> guard.postProcessEnvironment(production().withProperty(key, " "), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(key);
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.url", "spring.flyway.url"})
    void rejectsCredentialsInJdbcUrlsWithoutPrintingInput(String key) {
        String url = "jdbc:postgresql://127.0.0.1:15432/validation?%70assword=qa-marker";
        assertThatThrownBy(() -> guard.postProcessEnvironment(production().withProperty(key, url), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(key)
                .hasMessageNotContaining(url).hasMessageNotContaining("qa-marker")
                .hasNoCause();
    }

    @Test
    void rejectsInvalidJwtSecretWithoutPrintingInput() {
        String invalid = "invalid-qa-secret!";
        assertThatThrownBy(() -> guard.postProcessEnvironment(production().withProperty("jwt.secret", invalid), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret")
                .hasMessageNotContaining(invalid)
                .hasNoCause();
    }

    @Test
    void rejectsMixedProfilesBeforeReadingCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "dev");

        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cannot be active together");
    }

    @Test
    void supportsIsolatedSettingsAndExplicitPasswordlessDevelopment() {
        assertThatCode(() -> guard.postProcessEnvironment(production(), null)).doesNotThrowAnyException();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:15432/validation")
                .withProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.datasource.password");
        MockEnvironment separateFlyway = new MockEnvironment()
                .withProperty("spring.flyway.url", "jdbc:postgresql://127.0.0.1:15432/validation");
        assertThatThrownBy(() -> guard.postProcessEnvironment(separateFlyway, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.datasource.password");
        environment.setActiveProfiles("dev");
        assertThatCode(() -> guard.postProcessEnvironment(environment, null)).doesNotThrowAnyException();
    }

    private MockEnvironment production() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:configuration_guard")
                .withProperty("spring.datasource.username", "qa-user")
                .withProperty("spring.datasource.password", "qa-placeholder")
                .withProperty("jwt.secret", Base64.getEncoder().encodeToString(new byte[32]));
        environment.setActiveProfiles("prod");
        return environment;
    }
}
