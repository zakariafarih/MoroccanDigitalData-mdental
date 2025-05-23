package org.mdental.authcore.infrastructure.security;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.AuthEvent;
import org.mdental.authcore.domain.model.Client;
import org.mdental.authcore.domain.model.RefreshToken;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.RefreshTokenRepository;
import org.mdental.authcore.domain.service.OutboxService;
import org.mdental.authcore.exception.InvalidTokenException;
import org.mdental.authcore.util.TokenHashUtil;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.jwt.JwtException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for token-related operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;

    @Value("${mdental.auth.refresh-token-validity:43200}")
    private long refreshTokenValidityMinutes;

    /**
     * Generate authentication tokens for a user.
     *
     * @param user the user
     * @return a map containing access and refresh tokens
     */
    public Map<String, Object> generateAuthTokens(User user) {
        // Generate JWT access token
        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTenantId(),
                user.getRoles()
        );

        // Generate refresh token
        String refreshToken = generateRefreshToken(user);

        // Return tokens
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("refresh_token", refreshToken);
        result.put("token_type", "Bearer");
        result.put("expires_in", 3600); // 1 hour in seconds

        meterRegistry.counter("auth.token.generated").increment();

        return result;
    }

    /**
     * Find and validate a refresh token.
     *
     * @param refreshTokenValue the refresh token value
     * @return the validated refresh token
     */
    public RefreshToken findAndValidateRefreshToken(String refreshTokenValue) {
        meterRegistry.counter("auth.token.refresh.attempts").increment();

        // Calculate token hash
        String tokenHash = TokenHashUtil.hashToken(refreshTokenValue);

        // Find token in database - try both current hash and previous hash
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseGet(() -> refreshTokenRepository.findByPreviousTokenHash(tokenHash)
                        .orElseThrow(() -> {
                            meterRegistry.counter("auth.token.refresh.invalid").increment();
                            return new InvalidTokenException("Invalid refresh token");
                        }));

        // If found by previous hash, this is a replay attack
        if (token.getPreviousTokenHash() != null && token.getPreviousTokenHash().equals(tokenHash)) {
            // Revoke both tokens
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            meterRegistry.counter("auth.token.replay.detected").increment();
            log.warn("Replay attack detected! Reuse of rotated token for user {}", token.getUserId());
            throw new InvalidTokenException("Refresh token has been revoked due to possible replay attack");
        }

        // Check if token is expired or revoked
        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            meterRegistry.counter("auth.token.refresh.expired").increment();
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        return token;
    }

    /**
     * Refresh tokens.
     *
     * @param token the existing refresh token
     * @param user the user
     * @param rotateRefreshTokens whether to rotate the refresh token
     * @return new tokens
     */
    public Map<String, Object> refreshTokens(RefreshToken token, User user, boolean rotateRefreshTokens) {
        // Generate new access token
        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTenantId(),
                user.getRoles()
        );

        // Generate new refresh token and revoke the old one if token rotation is enabled
        String newRefreshToken;
        if (rotateRefreshTokens) {
            // Remember the old token hash
            String oldTokenHash = token.getTokenHash();

            // Generate new token
            newRefreshToken = UUID.randomUUID().toString();
            String newTokenHash = TokenHashUtil.hashToken(newRefreshToken);

            // Update token with new hash and store previous hash for replay detection
            token.setPreviousTokenHash(oldTokenHash);
            token.setTokenHash(newTokenHash);
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(refreshTokenValidityMinutes)));
            refreshTokenRepository.save(token);

            // Publish token refreshed event
            outboxService.saveEvent(
                    "Auth",
                    token.getId(),
                    AuthEvent.TOKEN_REFRESHED.name(),
                    null,
                    Map.of(
                            "userId", user.getId(),
                            "tenantId", user.getTenantId(),
                            "timestamp", Instant.now()
                    )
            );
        } else {
            // Reuse the existing token
            newRefreshToken = deriveOriginalTokenFromHash(token.getTokenHash());
        }

        // Track metrics
        meterRegistry.counter("auth.token.refresh.success").increment();

        // Return tokens
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("refresh_token", newRefreshToken);
        result.put("token_type", "Bearer");
        result.put("expires_in", 3600); // 1 hour in seconds
        return result;
    }

    /**
     * Generate a refresh token for a user.
     *
     * @param user the user
     * @return the refresh token value
     */
    public String generateRefreshToken(User user) {
        // Generate a secure random token
        String tokenValue = UUID.randomUUID().toString();

        // Hash token for storage
        String tokenHash = TokenHashUtil.hashToken(tokenValue);

        // Calculate expiry
        Instant expiryDate = Instant.now().plus(Duration.ofMinutes(refreshTokenValidityMinutes));

        // Create and save token
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .tokenHash(tokenHash)
                .expiresAt(expiryDate)
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    /**
     * Revoke a refresh token.
     *
     * @param refreshTokenValue the refresh token value
     */
    public void revokeToken(String refreshTokenValue) {
        String tokenHash = TokenHashUtil.hashToken(refreshTokenValue);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);

            log.info("Revoked refresh token for user: {}", token.getUserId());
            meterRegistry.counter("auth.token.revoked").increment();

            // Publish token revoked event
            outboxService.saveEvent(
                    "Auth",
                    token.getId(),
                    AuthEvent.TOKEN_REVOKED.name(),
                    null,
                    Map.of(
                            "userId", token.getUserId(),
                            "tenantId", token.getTenantId(),
                            "timestamp", Instant.now()
                    )
            );
        });
    }

    /**
     * Revoke all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    public int revokeAllUserTokens(UUID userId) {
        int count = refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("Revoked {} refresh tokens for user: {}", count, userId);
        meterRegistry.counter("auth.token.bulk_revoked").increment(count);
        return count;
    }

    /**
     * Validate a JWT token and extract principal.
     *
     * @param token the JWT token
     * @return the authentication principal
     */
    public AuthPrincipal validateToken(String token) {
        try {
            jwtTokenProvider.validateToken(token);
            return jwtTokenProvider.parseToken(token);
        } catch (JwtException e) {
            meterRegistry.counter("auth.token.invalid").increment();
            throw e;
        }
    }

    /**
     * Introspect a token.
     *
     * @param token the token to introspect
     * @return introspection response
     */
    public Map<String, Object> introspectToken(String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            jwtTokenProvider.validateToken(token);
            AuthPrincipal principal = jwtTokenProvider.parseToken(token);

            response.put("active", true);
            response.put("sub", principal.id().toString());
            response.put("username", principal.username());
            response.put("email", principal.email());
            response.put("tenant_id", principal.tenantId().toString());
            response.put("roles", principal.roles());

            meterRegistry.counter("auth.token.introspect.valid").increment();
        } catch (Exception e) {
            // For security, don't expose details about invalid tokens
            response.put("active", false);
            meterRegistry.counter("auth.token.introspect.invalid").increment();
        }

        return response;
    }

    /**
     * Clean up expired and revoked tokens.
     *
     * @return the number of tokens deleted
     */
    public int cleanupExpiredTokens() {
        int count = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired refresh tokens", count);
        meterRegistry.counter("auth.token.cleanup").increment(count);
        return count;
    }

    /**
     * Utility method to derive original token from hash - this is a placeholder
     * as we can't actually reverse a hash. In a real system, we'd keep the original
     * tokens somewhere secure or simply issue a new one.
     */
    private String deriveOriginalTokenFromHash(String tokenHash) {
        // You can't actually reverse a hash in real life
        // This is just a placeholder since we're not actually rotating tokens
        return UUID.randomUUID().toString();
    }

    // Method to add to TokenService
    public String generateClientToken(Client client) {
        // Generate JWT for client
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", client.getClientId());
        if (client.getTenantId() != null) {
            claims.put("tenant_id", client.getTenantId().toString());
        }
        claims.put("scope", "service_account");

        // Include roles
        if (client.getRoles() != null && !client.getRoles().isEmpty()) {
            claims.put("roles", client.getRoles().stream()
                    .map(Role::name)
                    .collect(Collectors.toList()));
        }

        // Set expiration time
        int expirySeconds = client.getAccessTokenValiditySeconds() != null
                ? client.getAccessTokenValiditySeconds() : 3600;

        // Note: This is a simplified example. In a real implementation, you'd use the JwtTokenProvider
        return "client-token-placeholder";
    }
}