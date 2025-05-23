package org.mdental.authcore.infrastructure.security;

import jakarta.annotation.PostConstruct;
import java.io.File;
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

    @PostConstruct
    public void init() {
        Path keyDirectory = Paths.get(keyDirectoryPath);
        Path privateKeyPath = keyDirectory.resolve("private.pem");
        Path publicKeyPath = keyDirectory.resolve("public.pem");

        try {
            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                loadKeys(privateKeyPath, publicKeyPath);
            } else if (generateIfMissing) {
                generateAndSaveKeys(keyDirectory, privateKeyPath, publicKeyPath);
            } else {
                throw new IllegalStateException("RSA keys not found at " + keyDirectoryPath);
            }
        } catch (Exception e) {
            log.error("Failed to initialize RSA keys", e);
            throw new RuntimeException("Failed to initialize RSA keys", e);
        }
    }

    private void loadKeys(Path privateKeyPath, Path publicKeyPath) throws IOException {
        // Load keys from files
        String privateKeyContent = Files.readString(privateKeyPath);
        String publicKeyContent = Files.readString(publicKeyPath);

        // Parse keys
        parseKeys(privateKeyContent, publicKeyContent);

        log.info("Loaded RSA keys from {}", privateKeyPath.getParent());
    }

    private void generateAndSaveKeys(Path keyDirectory, Path privateKeyPath, Path publicKeyPath)
            throws NoSuchAlgorithmException, IOException {
        log.info("Generating new RSA key pair...");

        // Create directory if it doesn't exist
        Files.createDirectories(keyDirectory);

        // Generate keys
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Convert to PEM format
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        String privateKeyEncoded = encodePrivateKeyToPem(privateKey);
        String publicKeyEncoded = encodePublicKeyToPem(publicKey);

        // Save to files
        Files.writeString(privateKeyPath, privateKeyEncoded);
        Files.writeString(publicKeyPath, publicKeyEncoded);

        // Set permissions - private key should be readable only by the owner
        File privateKeyFile = privateKeyPath.toFile();
        privateKeyFile.setReadable(false, false);
        privateKeyFile.setReadable(true, true);
        privateKeyFile.setWritable(false, false);
        privateKeyFile.setWritable(true, true);

        log.info("Generated and saved RSA keys to {}", keyDirectory);
    }

    private void parseKeys(String privateKeyContent, String publicKeyContent) {
        try {
            // Parse private key
            String privateKeyBase64 = privateKeyContent
                    .replace(BEGIN_PRIVATE_KEY, "")
                    .replace(END_PRIVATE_KEY, "")
                    .replaceAll("\\s", "");

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            privateKey = parsePrivateKey(privateKeyBytes);

            // Parse public key
            String publicKeyBase64 = publicKeyContent
                    .replace(BEGIN_PUBLIC_KEY, "")
                    .replace(END_PUBLIC_KEY, "")
                    .replaceAll("\\s", "");

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            publicKey = parsePublicKey(publicKeyBytes);
        } catch (Exception e) {
            log.error("Failed to parse RSA keys", e);
            throw new RuntimeException("Failed to parse RSA keys", e);
        }
    }

    private RSAPrivateKey parsePrivateKey(byte[] privateKeyBytes) {
        // Implementation would depend on the key format
        // This is a placeholder
        return null;
    }

    private RSAPublicKey parsePublicKey(byte[] publicKeyBytes) {
        // Implementation would depend on the key format
        // This is a placeholder
        return null;
    }

    private String encodePrivateKeyToPem(RSAPrivateKey privateKey) {
        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return formatPemKey(base64Key, BEGIN_PRIVATE_KEY, END_PRIVATE_KEY);
    }

    private String encodePublicKeyToPem(RSAPublicKey publicKey) {
        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return formatPemKey(base64Key, BEGIN_PUBLIC_KEY, END_PUBLIC_KEY);
    }

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