package org.mdental.authcore.domain.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.AuthEvent;
import org.mdental.authcore.persistence.entity.FailedLoginAttempt;
import org.mdental.authcore.persistence.entity.RefreshToken;
import org.mdental.authcore.persistence.entity.User;
import org.mdental.authcore.persistence.repository.FailedLoginAttemptRepository;
import org.mdental.authcore.persistence.repository.RefreshTokenRepository;
import org.mdental.authcore.exception.AccountLockedException;
import org.mdental.authcore.exception.AuthenticationException;
import org.mdental.authcore.exception.InvalidTokenException;
import org.mdental.authcore.util.TokenHashUtil;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.security.jwt.JwtException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.mdental.security.password.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final PasswordService passwordService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FailedLoginAttemptRepository failedLoginRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;

    @Value("${mdental.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${mdental.auth.lockout-duration:15}")
    private int lockoutDurationMinutes;

    @Value("${mdental.auth.refresh-token-validity:43200}")
    private long refreshTokenValidityMinutes;

    @Value("${mdental.auth.rotate-refresh-tokens:true}")
    private boolean rotateRefreshTokens;

    /**
     * Authenticate a user and generate tokens.
     *
     * @param tenantId the tenant ID
     * @param username the username
     * @param password the password
     * @param ipAddress the client IP address
     * @return authentication response with tokens
     */
    @Transactional
    public Map<String, Object> authenticate(UUID tenantId, String username, String password, String ipAddress) {
        log.debug("Authenticating user: {} for tenant: {}", username, tenantId);

        // Track metrics
        meterRegistry.counter("auth.login.attempts", "tenant", tenantId.toString()).increment();

        // Check if too many failed attempts
        Instant lockoutThreshold = Instant.now().minus(Duration.ofMinutes(lockoutDurationMinutes));
        int recentFailures = failedLoginRepository.countRecentFailedAttempts(username, tenantId, lockoutThreshold);

        if (recentFailures >= maxFailedAttempts) {
            log.warn("Account locked due to too many failed attempts: {}", username);
            meterRegistry.counter("auth.login.lockouts", "tenant", tenantId.toString()).increment();
            throw new AccountLockedException("Account is temporarily locked due to too many failed login attempts");
        }

        try {
            // Find user
            User user = userService.findByUsernameAndTenantId(username, tenantId)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Check if account is locked
            if (user.isLocked()) {
                log.warn("Login attempt to locked account: {}", username);
                meterRegistry.counter("auth.login.locked", "tenant", tenantId.toString()).increment();
                throw new AccountLockedException("Account is locked");
            }

            // Verify password - use constant-time comparison
            if (!passwordService.matches(password.toCharArray(), user.getPasswordHash())) {
                recordFailedLogin(username, tenantId, ipAddress);
                throw new BadCredentialsException("Invalid credentials");
            }

            // Check if email is verified (skip for initial admin)
            if (!user.isEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", username);
                meterRegistry.counter("auth.login.unverified", "tenant", tenantId.toString()).increment();
                throw new AuthenticationException("Email not verified");
            }

            // Generate JWT tokens
            String accessToken = jwtTokenProvider.createToken(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getTenantId(),
                    user.getRoles()
            );

            // Generate refresh token
            String refreshToken = generateRefreshToken(user);

            // Record successful login
            userService.recordLogin(user.getId());

            // Publish login event
            outboxService.saveEvent(
                    "User",
                    user.getId(),
                    AuthEvent.LOGIN_SUCCESS.name(),
                    null,
                    Map.of(
                            "userId", user.getId(),
                            "tenantId", user.getTenantId(),
                            "timestamp", Instant.now()
                    )
            );

            // Track metrics
            meterRegistry.counter("auth.login.success", "tenant", tenantId.toString()).increment();

            // Return tokens
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("refresh_token", refreshToken);
            result.put("token_type", "Bearer");
            result.put("expires_in", 3600); // 1 hour in seconds
            return result;

        } catch (BadCredentialsException e) {
            recordFailedLogin(username, tenantId, ipAddress);
            throw new AuthenticationException("Invalid username or password");
        }
    }

    /**
     * Refresh an access token using a refresh token.
     * Implements token rotation for security.
     *
     * @param refreshTokenValue the refresh token value
     * @return new tokens
     */
    @Transactional
    public Map<String, Object> refreshToken(String refreshTokenValue) {
        log.debug("Refreshing token");

        // Track metrics
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

        // Find user
        User user = userService.getUserById(token.getUserId());

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
            newRefreshToken = refreshTokenValue;
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
     * Revoke a refresh token.
     *
     * @param refreshTokenValue the refresh token value
     */
    @Transactional
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
    @Transactional
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
        } catch (Exception e) {
            // For security, don't expose details about invalid tokens
            response.put("active", false);
        }

        return response;
    }

    /**
     * Record a failed login attempt.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @param ipAddress the client IP address
     */
    private void recordFailedLogin(String username, UUID tenantId, String ipAddress) {
        log.info("Failed login attempt for user: {} in tenant: {}", username, tenantId);

        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
                .username(username)
                .tenantId(tenantId)
                .ipAddress(ipAddress)
                .attemptedAt(Instant.now())
                .build();

        failedLoginRepository.save(attempt);

        // Track metrics
        meterRegistry.counter("auth.login.failed", "tenant", tenantId.toString()).increment();

        // Publish failed login event
        outboxService.saveEvent(
                "Auth",
                UUID.randomUUID(), // No specific aggregate ID
                AuthEvent.LOGIN_FAILED.name(),
                null,
                Map.of(
                        "username", username,
                        "tenantId", tenantId,
                        "timestamp", Instant.now()
                )
        );
    }

    /**
     * Generate a refresh token for a user.
     *
     * @param user the user
     * @return the refresh token value
     */
    private String generateRefreshToken(User user) {
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
     * Clean up expired and revoked tokens.
     *
     * @return the number of tokens deleted
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int count = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired refresh tokens", count);
        meterRegistry.counter("auth.token.cleanup").increment(count);
        return count;
    }
}
