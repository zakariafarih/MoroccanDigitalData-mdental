package org.mdental.authcore.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mdental.authcore.service.SecretManager;

class SecretManagerTest {

    @Test
    void encryptAndDecrypt() {
        // Arrange
        SecretManager secretManager = new SecretManager("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=", true);
        String plaintext = "test-secret-value";

        // Act
        String encrypted = secretManager.encryptSecret(plaintext);
        String decrypted = secretManager.decryptSecret(encrypted);

        // Assert
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void handlesNullValues() {
        // Arrange
        SecretManager secretManager = new SecretManager("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=", true);

        // Act & Assert
        assertThat(secretManager.encryptSecret(null)).isNull();
        assertThat(secretManager.decryptSecret(null)).isNull();
    }

    @Test
    void disabledEncryption() {
        // Arrange
        SecretManager secretManager = new SecretManager("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=", false);
        String plaintext = "test-secret-value";

        // Act
        String encrypted = secretManager.encryptSecret(plaintext);
        String decrypted = secretManager.decryptSecret(encrypted);

        // Assert
        assertThat(encrypted).isEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }
}