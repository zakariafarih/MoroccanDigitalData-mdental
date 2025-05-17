package org.mdental.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakRealmJwtIssuerValidatorTest {

    private KeycloakRealmJwtIssuerValidator validator;
    private List<String> allowedPatterns;

    @BeforeEach
    void setup() {
        // Setup allowed patterns for testing
        allowedPatterns = Arrays.asList(
                "https://auth.mdental.org/realms/platform",  // Exact match
                "https://auth.mdental.org/realms/mdental-*"  // Wildcard match
        );
        validator = new KeycloakRealmJwtIssuerValidator(allowedPatterns);
    }

    @ParameterizedTest
    @CsvSource({
            "https://auth.mdental.org/realms/platform, true",            // Exact match
            "https://auth.mdental.org/realms/mdental-demo, true",        // Matches wildcard
            "https://auth.mdental.org/realms/mdental-clinic123, true",   // Matches wildcard
            "https://auth.mdental.org/realms/other-realm, false",        // No match
            "https://auth.mdental.org/realms, false",                    // No match
            "https://evil.com/realms/mdental-clinic, false",             // No match - different host
            "https://auth.mdental.org/realms/mdental, false"             // No match - prefix but not wildcard
    })
    void testValidateJwt(String issuer, boolean expected) throws MalformedURLException {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getIssuer()).thenReturn(URI.create(issuer).toURL());

        // Act
        var result = validator.validate(jwt);

        // Assert
        if (expected) {
            assertFalse(result.hasErrors(), "Validation should succeed for " + issuer);
        } else {
            assertTrue(result.hasErrors(), "Validation should fail for " + issuer);
        }
    }

    @Test
    void testValidationWithExactMatchingPattern() throws MalformedURLException {
        // Arrange
        List<String> exactPatterns = List.of("https://auth.mdental.org/realms/exact-realm");
        KeycloakRealmJwtIssuerValidator exactValidator = new KeycloakRealmJwtIssuerValidator(exactPatterns);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getIssuer()).thenReturn(URI.create("https://auth.mdental.org/realms/exact-realm").toURL());

        // Act
        var result = exactValidator.validate(jwt);

        // Assert
        assertFalse(result.hasErrors(), "Validation should succeed for exact match");
    }
}