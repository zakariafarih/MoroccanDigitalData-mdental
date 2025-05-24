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
    private static final String END_PRIVATE_KEY   = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY  = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY    = "-----END PUBLIC KEY-----";

    @Value("${mdental.auth.keys.path:./secret/keys}")
    private String keyDirectoryPath;

    @Getter
    private RSAPrivateKey privateKey;

    @Getter
    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() {
        Path keyDir   = Paths.get(keyDirectoryPath);
        Path privPath = keyDir.resolve("private.pem");
        Path pubPath  = keyDir.resolve("public.pem");

        try {
            Files.createDirectories(keyDir);
            if (Files.exists(privPath) && Files.exists(pubPath)) {
                log.info("Loading RSA keys from {}", keyDir.toAbsolutePath());
                loadKeys(privPath, pubPath);
            } else {
                log.info("Generating ephemeral in-memory RSA key pair");
                generateKeysInMemory();
            }
        } catch (Exception ex) {
            log.error("❌ Failed to initialize RSA keys – {}", ex.getMessage(), ex);
            throw new RuntimeException("RSA key initialization failed", ex);
        }
    }

    private void loadKeys(Path privateKeyPath, Path publicKeyPath) throws IOException, NoSuchAlgorithmException {
        String privatePem = Files.readString(privateKeyPath);
        String publicPem  = Files.readString(publicKeyPath);

        try {
            this.privateKey = parsePrivateKeyFromPem(privatePem);
            this.publicKey  = parsePublicKeyFromPem(publicPem);
            log.info("Successfully loaded RSA keys");
        } catch (Exception e) {
            log.warn("Parsing of PEM files failed, falling back to in-memory generation", e);
            generateKeysInMemory();
        }
    }

    private void generateKeysInMemory() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        this.privateKey = (RSAPrivateKey) kp.getPrivate();
        this.publicKey  = (RSAPublicKey) kp.getPublic();
    }

    private RSAPrivateKey parsePrivateKeyFromPem(String pemContent) {
        String base64 = pemContent
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        // real implementation would use PKCS8EncodedKeySpec + KeyFactory here
        return (RSAPrivateKey) KeyUtil.parsePrivateKey(decoded);
    }

    private RSAPublicKey parsePublicKeyFromPem(String pemContent) {
        String base64 = pemContent
                .replace(BEGIN_PUBLIC_KEY, "")
                .replace(END_PUBLIC_KEY, "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        // real implementation would use X509EncodedKeySpec + KeyFactory here
        return (RSAPublicKey) KeyUtil.parsePublicKey(decoded);
    }

    // These stubs assume you supply KeyUtil with real parsing logic.
    private static class KeyUtil {
        static java.security.PrivateKey parsePrivateKey(byte[] pkcs8Bytes) {
            throw new UnsupportedOperationException("Implement PKCS8 parsing");
        }
        static java.security.PublicKey parsePublicKey(byte[] x509Bytes) {
            throw new UnsupportedOperationException("Implement X509 parsing");
        }
    }
}
