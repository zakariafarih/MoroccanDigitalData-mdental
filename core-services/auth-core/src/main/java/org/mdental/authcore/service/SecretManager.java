package org.mdental.authcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class SecretManager {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final boolean encryptSecrets;
    private final SecretKey encryptionKey;

    public SecretManager(
            @Value("${mdental.security.encryption-key:}") String configuredKey,
            @Value("${mdental.security.encrypt-secrets:true}") boolean encryptSecrets
    ) {
        this.encryptSecrets = encryptSecrets;

        if (configuredKey != null && !configuredKey.isBlank()) {
            // Use the provided Base64-encoded key
            byte[] decodedKey = Base64.getDecoder().decode(configuredKey);
            this.encryptionKey = new SecretKeySpec(decodedKey, "AES");
            log.info("Using configured encryption key for secrets");
        } else {
            // Generate a temporary key for development
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                this.encryptionKey = keyGen.generateKey();
                log.warn("Using temporary encryption key - NOT SUITABLE FOR PRODUCTION");
            } catch (Exception e) {
                log.error("Failed to generate encryption key", e);
                throw new RuntimeException("Could not initialize encryption", e);
            }
        }
    }

    /**
     * Encrypt a secret value
     *
     * @param plaintext The plaintext value to encrypt
     * @return The encrypted value (Base64-encoded)
     */
    public String encryptSecret(String plaintext) {
        if (!encryptSecrets) {
            return plaintext;
        }

        try {
            // Generate a random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // Combine IV + ciphertext
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            // Return as Base64
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error encrypting secret", e);
            throw new RuntimeException("Error encrypting secret", e);
        }
    }

    /**
     * Decrypt a secret value
     *
     * @param encryptedBase64 The encrypted value (Base64-encoded)
     * @return The plaintext value
     */
    public String decryptSecret(String encryptedBase64) {
        if (!encryptSecrets) {
            return encryptedBase64;
        }

        try {
            // Decode Base64
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);

            // Extract ciphertext
            byte[] ciphertext = new byte[encrypted.length - iv.length];
            System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext);
        } catch (Exception e) {
            log.error("Error decrypting secret", e);
            throw new RuntimeException("Error decrypting secret", e);
        }
    }
}
