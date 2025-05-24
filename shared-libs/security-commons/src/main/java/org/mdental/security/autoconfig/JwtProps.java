package org.mdental.security.autoconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for JWT functionality
 */
@ConfigurationProperties(prefix = "mdental.auth.jwt")
@Getter
@Setter
@Validated
@Schema(description = "JWT configuration properties")
public class JwtProps {

    /**
     * Secret key for HS256 signing (Base64 encoded)
     */
    private String secret;

    /**
     * Public key for RS256 verification (Base64 encoded)
     */
    private String publicKey;

    /**
     * Private key for RS256 signing (Base64 encoded)
     */
    private String privateKey;

    /**
     * Issuer identifier for tokens
     */
    @NotBlank(message = "Issuer is required")
    private String issuer = "mdental.org";

    /**
     * The key identifier (kid) to put in JWT headers.
     */
    @NotBlank(message = "Key ID is required")
    private String keyId;

    /**
     * Access token time-to-live in seconds
     */
    @Positive(message = "Access token TTL must be positive")
    private long accessTtl = 3600; // 1 hour

    /**
     * Refresh token time-to-live in seconds
     */
    @Positive(message = "Refresh token TTL must be positive")
    private long refreshTtl = 2592000; // 30 days

    /**
     * Determine if RSA (asymmetric) or HMAC (symmetric) signing should be used
     */
    public boolean isRsa() {
        return publicKey != null && !publicKey.isBlank() &&
                privateKey != null && !privateKey.isBlank();
    }
}
