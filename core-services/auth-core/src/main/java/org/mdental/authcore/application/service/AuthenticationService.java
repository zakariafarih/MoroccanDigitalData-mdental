package org.mdental.authcore.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.AuthEvent;
import org.mdental.authcore.domain.model.FailedLoginAttempt;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.FailedLoginAttemptRepository;
import org.mdental.authcore.domain.service.AuthService;
import org.mdental.authcore.domain.service.OutboxService;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.authcore.exception.AccountLockedException;
import org.mdental.authcore.exception.AuthenticationException;
import org.mdental.commons.model.AuthPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserService userService;
    private final AuthService authService;
    private final FailedLoginAttemptRepository failedLoginRepository;
    private final OutboxService outboxService;

    @Value("${mdental.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${mdental.auth.lockout-duration:15}")
    private int lockoutDurationMinutes;

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

        // Check if too many failed attempts
        Instant lockoutThreshold = Instant.now().minus(Duration.ofMinutes(lockoutDurationMinutes));
        int recentFailures = failedLoginRepository.countRecentFailedAttempts(username, tenantId, lockoutThreshold);

        if (recentFailures >= maxFailedAttempts) {
            log.warn("Account locked due to too many failed attempts: {}", username);
            throw new AccountLockedException("Account is temporarily locked due to too many failed login attempts");
        }

        try {
            // Find user
            User user = userService.findByUsernameAndTenantId(username, tenantId)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Check if account is locked
            if (user.isLocked()) {
                log.warn("Login attempt to locked account: {}", username);
                throw new AccountLockedException("Account is locked");
            }

            // Verify password
            if (!userService.verifyPassword(user, password)) {
                recordFailedLogin(username, tenantId, ipAddress);
                throw new BadCredentialsException("Invalid credentials");
            }

            // Check if email is verified (skip for initial admin)
            if (!user.isEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", username);
                throw new AuthenticationException("Email not verified");
            }

            // Use AuthService to authenticate and generate tokens
            Map<String, Object> tokens = authService.authenticate(tenantId, username, password, ipAddress);

            return tokens;

        } catch (BadCredentialsException e) {
            recordFailedLogin(username, tenantId, ipAddress);
            throw new AuthenticationException("Invalid username or password");
        }
    }

    /**
     * Refresh an access token using a refresh token.
     *
     * @param refreshTokenValue the refresh token value
     * @return new tokens
     */
    @Transactional
    public Map<String, Object> refreshToken(String refreshTokenValue) {
        log.debug("Refreshing token");
        return authService.refreshToken(refreshTokenValue);
    }

    /**
     * Revoke a refresh token.
     *
     * @param refreshTokenValue the refresh token value
     */
    @Transactional
    public void revokeToken(String refreshTokenValue) {
        authService.revokeToken(refreshTokenValue);
    }

    /**
     * Revoke all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    @Transactional
    public int revokeAllUserTokens(UUID userId) {
        return authService.revokeAllUserTokens(userId);
    }

    /**
     * Validate a JWT token and extract principal.
     *
     * @param token the JWT token
     * @return the authentication principal
     */
    public AuthPrincipal validateToken(String token) {
        return authService.validateToken(token);
    }

    /**
     * Introspect a token.
     *
     * @param token the token to introspect
     * @return introspection response
     */
    public Map<String, Object> introspectToken(String token) {
        return authService.introspectToken(token);
    }

    /**
     * Clean up expired and revoked tokens.
     *
     * @return the number of tokens deleted
     */
    @Transactional
    public int cleanupExpiredTokens() {
        return authService.cleanupExpiredTokens();
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
}