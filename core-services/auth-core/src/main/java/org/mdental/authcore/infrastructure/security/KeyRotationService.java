package org.mdental.authcore.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeyRotationService {
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    @Value("${mdental.auth.keys.path:./secret/keys}")
    private String keyDirectoryPath;

    @Value("${mdental.auth.keys.rotation.enabled:true}")
    private boolean keyRotationEnabled;

    @Value("${mdental.auth.keys.rotation.check-interval-minutes:60}")
    private int checkIntervalMinutes;

    @Value("${mdental.auth.keys.rotation.key-lifetime-days:90}")
    private int keyLifetimeDays;

    private final Clock clock;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Active key pair
    @Getter
    private RSAPrivateKey currentPrivateKey;

    @Getter
    private RSAPublicKey currentPublicKey;

    @Getter
    private String currentKeyId;

    // Key history for verification (kid -> public key)
    private final Map<String, RSAPublicKey> keyHistory = new ConcurrentHashMap<>();

    // JWKS set that gets updated when keys rotate
    @Getter
    private JWKSet jwkSet;

    public KeyRotationService(Clock clock) {
        this.clock = clock;
    }

    @PostConstruct
    public void initialize() {
        try {
            // Initial key loading
            loadOrGenerateCurrentKeys();

            // Build initial JWKS set
            updateJwkSet();

            // Start rotation check scheduler if enabled
            if (keyRotationEnabled) {
                scheduler.scheduleAtFixedRate(
                        this::checkAndRotateKeys,
                        checkIntervalMinutes,
                        checkIntervalMinutes,
                        TimeUnit.MINUTES
                );
                log.info("Key rotation service started with check interval of {} minutes", checkIntervalMinutes);
            }
        } catch (Exception e) {
            log.error("Failed to initialize key rotation service", e);
            throw new RuntimeException("Key rotation service initialization failed", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Get a public key by its key ID.
     *
     * @param kid the key ID
     * @return the public key or null if not found
     */
    public RSAPublicKey getPublicKey(String kid) {
        if (kid == null) {
            return currentPublicKey;
        }

        if (kid.equals(currentKeyId)) {
            return currentPublicKey;
        }

        return keyHistory.get(kid);
    }

    /**
     * Check if keys need rotation and rotate if necessary.
     */
    public synchronized void checkAndRotateKeys() {
        try {
            // Check for manually rotated keys first (external rotation)
            if (checkForExternalRotation()) {
                log.info("Detected externally rotated keys, updated key material");
                return;
            }

            // Check if current key has reached its lifetime
            Path keyMetadataPath = Paths.get(keyDirectoryPath, "key-metadata.properties");
            Map<String, String> metadata = loadKeyMetadata(keyMetadataPath);

            String createdAtStr = metadata.get("created-at");
            if (createdAtStr != null) {
                Instant createdAt = Instant.parse(createdAtStr);
                Instant rotationThreshold = createdAt.plus(keyLifetimeDays, ChronoUnit.DAYS);

                if (clock.instant().isAfter(rotationThreshold)) {
                    log.info("Current key has reached its lifetime of {} days, rotating keys", keyLifetimeDays);
                    rotateKeys();
                }
            } else {
                // No creation timestamp, set it now
                metadata.put("created-at", clock.instant().toString());
                saveKeyMetadata(keyMetadataPath, metadata);
            }
        } catch (Exception e) {
            log.error("Error during key rotation check", e);
        }
    }

    /**
     * Force key rotation.
     */
    public synchronized void rotateKeys() throws Exception {
        // Generate new key pair
        String newKeyId = "key-" + UUID.randomUUID().toString().substring(0, 8);
        KeyPair keyPair = generateKeyPair();
        RSAPrivateKey newPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey newPublicKey = (RSAPublicKey) keyPair.getPublic();

        // Save to filesystem with new key ID as part of filename
        Path keyDir = Paths.get(keyDirectoryPath);
        Files.createDirectories(keyDir);

        // Save private key
        String privateKeyPem = encodePrivateKeyToPem(newPrivateKey);
        Path privateKeyPath = keyDir.resolve(newKeyId + "-private.pem");
        Files.writeString(privateKeyPath, privateKeyPem);

        // Save public key
        String publicKeyPem = encodePublicKeyToPem(newPublicKey);
        Path publicKeyPath = keyDir.resolve(newKeyId + "-public.pem");
        Files.writeString(publicKeyPath, publicKeyPem);

        // Update symbolic links to point to new keys
        updateSymlinks(keyDir, newKeyId);

        // Add current key to history
        if (currentKeyId != null) {
            keyHistory.put(currentKeyId, currentPublicKey);
        }

        // Update current keys
        currentPrivateKey = newPrivateKey;
        currentPublicKey = newPublicKey;
        currentKeyId = newKeyId;

        // Update key metadata
        Path keyMetadataPath = keyDir.resolve("key-metadata.properties");
        Map<String, String> metadata = loadKeyMetadata(keyMetadataPath);
        metadata.put("current-key", newKeyId);
        metadata.put("created-at", clock.instant().toString());
        saveKeyMetadata(keyMetadataPath, metadata);

        // Update JWKS set
        updateJwkSet();

        log.info("Successfully rotated keys, new key ID: {}", newKeyId);
    }

    /**
     * Check if keys have been externally rotated (e.g., by an operator).
     *
     * @return true if keys were updated
     */
    private boolean checkForExternalRotation() throws Exception {
        Path keyDir = Paths.get(keyDirectoryPath);
        if (!Files.exists(keyDir)) {
            return false;
        }

        // Check metadata for current key ID
        Path keyMetadataPath = keyDir.resolve("key-metadata.properties");
        if (!Files.exists(keyMetadataPath)) {
            return false;
        }

        Map<String, String> metadata = loadKeyMetadata(keyMetadataPath);
        String metadataKeyId = metadata.get("current-key");

        // If metadata key ID doesn't match our current key ID, keys were rotated externally
        if (metadataKeyId != null && !metadataKeyId.equals(currentKeyId)) {
            // Load the externally rotated keys
            Path privateKeyPath = keyDir.resolve(metadataKeyId + "-private.pem");
            Path publicKeyPath = keyDir.resolve(metadataKeyId + "-public.pem");

            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                // Add current key to history
                if (currentKeyId != null) {
                    keyHistory.put(currentKeyId, currentPublicKey);
                }

                // Load new keys
                String privateKeyPem = Files.readString(privateKeyPath);
                String publicKeyPem = Files.readString(publicKeyPath);

                RSAPrivateKey newPrivateKey = parsePrivateKey(privateKeyPem);
                RSAPublicKey newPublicKey = parsePublicKey(publicKeyPem);

                // Update current keys
                currentPrivateKey = newPrivateKey;
                currentPublicKey = newPublicKey;
                currentKeyId = metadataKeyId;

                // Update JWKS set
                updateJwkSet();

                return true;
            }
        }

        return false;
    }

    /**
     * Load or generate keys on startup.
     */
    private void loadOrGenerateCurrentKeys() throws Exception {
        Path keyDir = Paths.get(keyDirectoryPath);
        if (!Files.exists(keyDir)) {
            Files.createDirectories(keyDir);
        }

        // Check for key metadata
        Path keyMetadataPath = keyDir.resolve("key-metadata.properties");
        Map<String, String> metadata = loadKeyMetadata(keyMetadataPath);

        String metadataKeyId = metadata.get("current-key");
        if (metadataKeyId != null) {
            // Try to load existing keys
            Path privateKeyPath = keyDir.resolve(metadataKeyId + "-private.pem");
            Path publicKeyPath = keyDir.resolve(metadataKeyId + "-public.pem");

            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                // Load keys
                String privateKeyPem = Files.readString(privateKeyPath);
                String publicKeyPem = Files.readString(publicKeyPath);

                currentPrivateKey = parsePrivateKey(privateKeyPem);
                currentPublicKey = parsePublicKey(publicKeyPem);
                currentKeyId = metadataKeyId;

                log.info("Loaded existing keys with key ID: {}", metadataKeyId);
                return;
            }
        }

        // No valid keys found, generate new ones
        log.info("No valid keys found, generating new key pair");
        rotateKeys();
    }

    /**
     * Update symbolic links to point to the current keys.
     */
    private void updateSymlinks(Path keyDir, String keyId) throws IOException {
        // Create symbolic links for easy access to current keys
        Path privateKeySymlink = keyDir.resolve("private.pem");
        Path publicKeySymlink = keyDir.resolve("public.pem");

        // Remove existing symlinks if they exist
        Files.deleteIfExists(privateKeySymlink);
        Files.deleteIfExists(publicKeySymlink);

        // Create new symlinks
        Files.createSymbolicLink(privateKeySymlink, keyDir.resolve(keyId + "-private.pem"));
        Files.createSymbolicLink(publicKeySymlink, keyDir.resolve(keyId + "-public.pem"));
    }

    /**
     * Load key metadata from file.
     */
    private Map<String, String> loadKeyMetadata(Path path) throws IOException {
        Map<String, String> metadata = new LinkedHashMap<>();

        if (Files.exists(path)) {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    metadata.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        return metadata;
    }

    /**
     * Save key metadata to file.
     */
    private void saveKeyMetadata(Path path, Map<String, String> metadata) throws IOException {
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            content.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        Files.writeString(path, content.toString());
    }

    /**
     * Update the JWKS set with current and historical keys.
     */
    private void updateJwkSet() {
        try {
            // 1. Collect all RSAKey JSON strings
            List<String> jwkJsons = new ArrayList<>();

            // Current key
            jwkJsons.add(currentRsaKey().toJSONString());

            // Historical keys
            for (Map.Entry<String, RSAPublicKey> entry : keyHistory.entrySet()) {
                RSAKey hist = new RSAKey.Builder(entry.getValue())
                        .keyID(entry.getKey())
                        .keyUse(KeyUse.SIGNATURE)
                        .build();
                jwkJsons.add(hist.toJSONString());
            }

            // 2. Build the JWKS JSON array string
            String keysArray = jwkJsons.stream().collect(Collectors.joining(","));
            String jwksJson = "{\"keys\":[" + keysArray + "]}";

            // 3. Parse it
            this.jwkSet = JWKSet.parse(jwksJson);

            log.debug("Updated JWKS set with {} keys", jwkJsons.size());
        } catch (Exception e) {
            log.error("Failed to update JWKS set", e);
        }
    }

    // Helper to build the current RSAKey
    private RSAKey currentRsaKey() {
        return new RSAKey.Builder(currentPublicKey)
                .keyID(currentKeyId)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    /**
     * Generate a new RSA key pair.
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Parse a PEM-encoded private key.
     */
    private RSAPrivateKey parsePrivateKey(String pemKey) throws Exception {
        String privateKeyPEM = pemKey
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * Parse a PEM-encoded public key.
     */
    private RSAPublicKey parsePublicKey(String pemKey) throws Exception {
        String publicKeyPEM = pemKey
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Encode a private key to PEM format.
     */
    private String encodePrivateKeyToPem(RSAPrivateKey privateKey) {
        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return formatPemKey(base64Key, BEGIN_PRIVATE_KEY, END_PRIVATE_KEY);
    }

    /**
     * Encode a public key to PEM format.
     */
    private String encodePublicKeyToPem(RSAPublicKey publicKey) {
        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return formatPemKey(base64Key, BEGIN_PUBLIC_KEY, END_PUBLIC_KEY);
    }

    /**
     * Format a Base64-encoded key as PEM.
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