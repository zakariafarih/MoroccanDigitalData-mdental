package org.mdental.authcore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerValidatorTest {

    @Test
    void shouldAllowMatchingIssuer() {
        // Arrange
        IssuerValidator validator = new IssuerValidator(
                List.of("http://localhost:9080/realms/platform", "http://localhost:9080/realms/mdental-.*"));

        // Act & Assert
        assertThat(validator.test("http://localhost:9080/realms/platform")).isTrue();
        assertThat(validator.test("http://localhost:9080/realms/mdental-clinic1")).isTrue();
        assertThat(validator.test("http://localhost:9080/realms/mdental-test-realm")).isTrue();
    }

    @Test
    void shouldRejectNonMatchingIssuer() {
        // Arrange
        IssuerValidator validator = new IssuerValidator(
                List.of("http://localhost:9080/realms/platform", "http://localhost:9080/realms/mdental-.*"));

        // Act & Assert
        assertThat(validator.test("http://malicious-site.com/realms/platform")).isFalse();
        assertThat(validator.test("http://localhost:9080/realms/hacked")).isFalse();
        assertThat(validator.test("http://localhost:9080/mdental-clinic1")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldRejectEmptyOrBlankIssuer(String issuer) {
        // Arrange
        IssuerValidator validator = new IssuerValidator(
                List.of("http://localhost:9080/realms/platform", "http://localhost:9080/realms/mdental-.*"));

        // Act & Assert
        assertThat(validator.test(issuer)).isFalse();
        assertThat(validator.test(null)).isFalse();
    }
}