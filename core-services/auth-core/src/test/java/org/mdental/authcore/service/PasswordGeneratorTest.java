package org.mdental.authcore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mdental.authcore.util.PasswordGenerator;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Test
    void shouldGeneratePasswordOfRequestedLength() {
        // Act
        String password = passwordGenerator.generateStrongPassword(20);

        // Assert
        assertThat(password).hasSize(20);
    }

    @Test
    void shouldEnforceMininumLength() {
        // Act - Try to generate a password shorter than minimum
        String password = passwordGenerator.generateStrongPassword(8);

        // Assert - Should enforce minimum length of 12
        assertThat(password).hasSize(12);
    }

    @Test
    void shouldIncludeLowercase() {
        // Act
        String password = passwordGenerator.generateStrongPassword(16);

        // Assert
        assertThat(password).matches(".*[a-z].*");
    }

    @Test
    void shouldIncludeUppercase() {
        // Act
        String password = passwordGenerator.generateStrongPassword(16);

        // Assert
        assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    void shouldIncludeDigits() {
        // Act
        String password = passwordGenerator.generateStrongPassword(16);

        // Assert
        assertThat(password).matches(".*[0-9].*");
    }

    @Test
    void shouldIncludeSpecialChars() {
        // Act
        String password = passwordGenerator.generateStrongPassword(16);

        // Assert - At least one special character
        Pattern specialChars = Pattern.compile(".*[!@#$%^&*()\\-_=+\\[\\]{}|;:,.<>?].*");
        assertThat(specialChars.matcher(password).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 16, 20, 32})
    void shouldGenerateUniquePasswords(int length) {
        // Act
        String password1 = passwordGenerator.generateStrongPassword(length);
        String password2 = passwordGenerator.generateStrongPassword(length);

        // Assert
        assertThat(password1).isNotEqualTo(password2);
    }
}