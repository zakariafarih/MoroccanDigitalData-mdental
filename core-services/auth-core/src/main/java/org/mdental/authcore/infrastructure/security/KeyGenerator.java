package org.mdental.authcore.infrastructure.security;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates and manages RSA keys for JWT signing.
 * Simplified implementation with in-memory key management.
 */
@Component
@Slf4j
public class KeyGenerator {
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    @Value("${mdental.auth.keys.path:./secret/keys}")
    private String keyDirectoryPath;

    @Value("${mdental.auth.keys.generate-if-missing:true}")
    private boolean generateIfMissing;

    @Getter
    private RSAPrivateKey privateKey;

    @Getter
    private RSAPublicKey publicKey;

    /**
     * Initialize the key generator.
     */
    @PostConstruct
    public void init() {
        Path keyDirectory = Paths.get(keyDirectoryPath);
        Path privateKeyPath = keyDirectory.resolve("private.pem");
        Path publicKeyPath = keyDirectory.resolve("public.pem");

        try {
            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                // Load keys from files
                log.info("Loading RSA keys from {}", keyDirectoryPath);
                loadKeys(privateKeyPath, publicKeyPath);
            } else if (generateIfMissing) {
                // Generate new keys
                log.info("Generating new RSA key pair");
                generateAndSaveKeys(keyDirectory, privateKeyPath, publicKeyPath);
            } else {
                throw new IllegalStateException("RSA keys not found at " + keyDirectoryPath);
            }
        } catch (Exception e) {
            log.error("Failed to initialize RSA keys", e);
            throw new RuntimeException("Failed to initialize RSA keys", e);
        }
    }

    /**
     * Load keys from files.
     *
     * @param privateKeyPath the private key file path
     * @param publicKeyPath the public key file path
     * @throws IOException if an error occurs reading the files
     */
    private void loadKeys(Path privateKeyPath, Path publicKeyPath) throws IOException {
        // Load key contents from files
        String privateKeyContent = Files.readString(privateKeyPath);
        String publicKeyContent = Files.readString(publicKeyPath);

        // Parse keys (implementation depends on your key format)
        // This is a simplified version - in a real implementation you'd use proper key parsing
        try {
            // Here we assume properly formatted PEM files
            this.privateKey = parsePrivateKeyFromPem(privateKeyContent);
            this.publicKey = parsePublicKeyFromPem(publicKeyContent);

            log.info("Successfully loaded RSA keys");
        } catch (Exception e) {
            log.error("Failed to parse RSA keys", e);
            throw new IOException("Failed to parse RSA keys", e);
        }
    }

    /**
     * Generate new keys and save them to files.
     *
     * @param keyDirectory the key directory
     * @param privateKeyPath the private key file path
     * @param publicKeyPath the public key file path
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     * @throws IOException if an error occurs writing the files
     */
    private void generateAndSaveKeys(Path keyDirectory, Path privateKeyPath, Path publicKeyPath)
            throws NoSuchAlgorithmException, IOException {
        // Create directory if it doesn't exist
        Files.createDirectories(keyDirectory);

        // Generate RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Cast to RSA keys
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        // Format keys as PEM and save to files
        String privateKeyPem = formatPrivateKeyAsPem(privateKey);
        String publicKeyPem = formatPublicKeyAsPem(publicKey);

        Files.writeString(privateKeyPath, privateKeyPem);
        Files.writeString(publicKeyPath, publicKeyPem);

        log.info("Generated and saved new RSA keys to {}", keyDirectory);

        // Add information about manual key rotation
        String rotationInstructions =
                "# RSA Key Rotation Instructions\n\n" +
                        "To rotate keys manually, follow these steps:\n\n" +
                        "1. Generate new key pair using OpenSSL:\n" +
                        "   ```\n" +
                        "   openssl genrsa -out private_new.pem 2048\n" +
                        "   openssl rsa -in private_new.pem -pubout -out public_new.pem\n" +
                        "   ```\n\n" +
                        "2. Back up the current keys:\n" +
                        "   ```\n" +
                        "   cp private.pem private_old.pem\n" +
                        "   cp public.pem public_old.pem\n" +
                        "   ```\n\n" +
                        "3. Replace the current keys with the new ones:\n" +
                        "   ```\n" +
                        "   cp private_new.pem private.pem\n" +
                        "   cp public_new.pem public.pem\n" +
                        "   ```\n\n" +
                        "4. Restart the service to load the new keys\n\n" +
                        "5. Keep the old public key available for token verification\n";

        Files.writeString(keyDirectory.resolve("ROTATION_INSTRUCTIONS.md"), rotationInstructions);
    }

    /**
     * Parse a private key from PEM format.
     *
     * @param pemContent the PEM content
     * @return the RSA private key
     */
    private RSAPrivateKey parsePrivateKeyFromPem(String pemContent) {
        // Remove PEM headers and decode Base64
        String base64Key = pemContent
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s+", "");

        // Actual implementation would use PKCS8EncodedKeySpec to parse the key
        // This is a placeholder
        return null; // In real implementation, parse the key
    }

    /**
     * Parse a public key from PEM format.
     *
     * @param pemContent the PEM content
     * @return the RSA public key
     */
    private RSAPublicKey parsePublicKeyFromPem(String pemContent) {
        // Remove PEM headers and decode Base64
        String base64Key = pemContent
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replaceAll("\\s+", "");

        // Actual implementation would use X509EncodedKeySpec to parse the key
        // This is a placeholder
        return null; // In real implementation, parse the key
    }

    /**
     * Format a private key as PEM.
     *
     * @param privateKey the private key
     * @return the PEM-formatted key
     */
    private String formatPrivateKeyAsPem(RSAPrivateKey privateKey) {
        // Encode key and format as PEM
        byte[] encodedKey = privateKey.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(encodedKey);

        return formatPemKey(base64Key, BEGIN_PRIVATE_KEY, END_PRIVATE_KEY);
    }

    /**
     * Format a public key as PEM.
     *
     * @param publicKey the public key
     * @return the PEM-formatted key
     */
    private String formatPublicKeyAsPem(RSAPublicKey publicKey) {
        // Encode key and format as PEM
        byte[] encodedKey = publicKey.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(encodedKey);

        return formatPemKey(base64Key, BEGIN_PUBLIC_KEY, END_PUBLIC_KEY);
    }

    /**
     * Format a Base64-encoded key as PEM.
     *
     * @param base64Key the Base64-encoded key
     * @param beginMarker the PEM begin marker
     * @param endMarker the PEM end marker
     * @return the PEM-formatted key
     */
    private String formatPemKey(String base64Key, String beginMarker, String endMarker) {
        StringBuilder result = new StringBuilder();
        result.append(beginMarker).append("\n");

        // Insert line breaks every 64 characters
        int offset = 0;
        while (offset < base64Key.length()) {
            int lineEnd = Math.min(offset + 64, base64Key.length());
            result.append(base64Key, offset, lineEnd).append("\n");
            offset = lineEnd;
        }

        result.append(endMarker).append("\n");
        return result.toString();
    }
}