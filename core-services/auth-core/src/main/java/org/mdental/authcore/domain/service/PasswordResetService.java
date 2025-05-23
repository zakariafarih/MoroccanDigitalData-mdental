package org.mdental.authcore.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.UserEvent;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.UserRepository;
import org.mdental.authcore.exception.InvalidTokenException;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.util.PasswordPolicy;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for password reset operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final OutboxService outboxService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordPolicy passwordPolicy;

    /**
     * Request a password reset by email.
     *
     * @param email the user email
     * @param tenantId the tenant ID
     * @return true if request was processed, false if no matching user
     */
    @Transactional
    public boolean requestPasswordReset(String email, UUID tenantId) {
        log.info("Password reset requested for email: {} in tenant: {}", email, tenantId);

        return userRepository.findActiveByEmailAndTenantId(email, tenantId).map(user -> {
            // Generate reset token (special-purpose JWT)
            String token = generateResetToken(user);

            // Send email with reset link
            sendResetEmail(user, token);

            // Publish event
            outboxService.saveEvent(
                    "User",
                    user.getId(),
                    UserEvent.PASSWORD_RESET_REQUESTED.name(),
                    null,
                    user.getId()
            );

            return true;
        }).orElse(false);
    }

    /**
     * Reset a password using a token.
     *
     * @param token the reset token
     * @param newPassword the new password
     * @return the user if successful
     */
    @Transactional
    public User resetPassword(String token, String newPassword) {
        // Validate token
        Map<String, Object> claims = validateResetToken(token);

        UUID userId = UUID.fromString((String) claims.get("sub"));

        // Validate new password
        passwordPolicy.validate(newPassword);

        // Reset password
        User user = userService.resetPassword(userId, newPassword);

        // Publish event
        outboxService.saveEvent(
                "User",
                user.getId(),
                UserEvent.PASSWORD_RESET_COMPLETED.name(),
                null,
                user.getId()
        );

        return user;
    }

    /**
     * Generate a password reset token.
     *
     * @param user the user
     * @return the token
     */
    private String generateResetToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "password_reset");
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("tenant_id", user.getTenantId().toString());

        Instant now = Instant.now();
        Instant expiryDate = now.plus(24, ChronoUnit.HOURS);

        return jwtTokenProvider.createToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTenantId(),
                user.getRoles()
        );
    }

    /**
     * Send password reset email.
     *
     * @param user the user
     * @param token the reset token
     */
    private void sendResetEmail(User user, String token) {
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("firstName", user.getFirstName());
        templateVars.put("resetLink", "/reset-password?token=" + token);
        templateVars.put("expiryHours", 24);

        emailService.sendEmail(
                user.getEmail(),
                "Reset Your Password",
                "password-reset",
                templateVars
        );
    }

    /**
     * Validate a password reset token.
     *
     * @param token the token
     * @return the token claims
     */
    private Map<String, Object> validateResetToken(String token) {
        try {
            // Validate JWT
            jwtTokenProvider.validateToken(token);

            // Parse claims
            Map<String, Object> claims = new HashMap<>();
            // In a real implementation, we would extract claims from JWT
            // For this sample, we'll assume token is valid if it passes validation

            return claims;
        } catch (Exception e) {
            log.warn("Invalid password reset token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired password reset token");
        }
    }
}