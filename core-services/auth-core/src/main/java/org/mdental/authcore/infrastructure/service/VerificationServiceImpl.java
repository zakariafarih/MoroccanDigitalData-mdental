package org.mdental.authcore.infrastructure.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.UserEvent;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.model.VerificationToken;
import org.mdental.authcore.domain.repository.VerificationTokenRepository;
import org.mdental.authcore.domain.service.EmailService;
import org.mdental.authcore.domain.service.OutboxService;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.authcore.domain.service.VerificationService;
import org.mdental.authcore.exception.InvalidTokenException;
import org.mdental.authcore.util.TokenHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of verification token operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final OutboxService outboxService;

    @Value("${mdental.auth.verification-token-validity:1440}")
    private long tokenValidityMinutes;

    /**
     * Create email verification token and send email.
     *
     * @param user the user
     * @return the token
     */
    @Override
    @Transactional
    public String createEmailVerificationToken(User user) {
        String token = createToken(user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION);

        // Send verification email
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("name", user.getFirstName());
        templateVars.put("verificationLink", "/verify-email?token=" + token);
        templateVars.put("expiryHours", tokenValidityMinutes / 60);

        emailService.sendEmail(
                user.getEmail(),
                "Verify Your Email Address",
                "email-verification",
                templateVars
        );

        return token;
    }

    /**
     * Create password reset token and send email.
     *
     * @param user the user
     * @return the token
     */
    @Override
    @Transactional
    public String createPasswordResetToken(User user) {
        String token = createToken(user.getId(), VerificationToken.TokenType.PASSWORD_RESET);

        // Send password reset email
        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("name", user.getFirstName());
        templateVars.put("resetLink", "/reset-password?token=" + token);
        templateVars.put("expiryHours", tokenValidityMinutes / 60);

        emailService.sendEmail(
                user.getEmail(),
                "Reset Your Password",
                "password-reset",
                templateVars
        );

        // Publish event
        outboxService.saveEvent(
                "User",
                user.getId(),
                UserEvent.PASSWORD_RESET_REQUESTED.name(),
                null,
                user.getId()
        );

        return token;
    }

    /**
     * Verify email using token.
     *
     * @param token the verification token
     * @return the user
     */
    @Override
    @Transactional
    public User verifyEmail(String token) {
        VerificationToken verificationToken = verifyToken(token, VerificationToken.TokenType.EMAIL_VERIFICATION);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        // Verify email
        User user = userService.verifyEmail(verificationToken.getUserId());

        // Publish event
        outboxService.saveEvent(
                "User",
                user.getId(),
                UserEvent.EMAIL_VERIFIED.name(),
                null,
                user.getId()
        );

        return user;
    }

    /**
     * Reset password using token.
     *
     * @param token the reset token
     * @param newPassword the new password
     * @return the user
     */
    @Override
    @Transactional
    public User resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = verifyToken(token, VerificationToken.TokenType.PASSWORD_RESET);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        // Reset password
        User user = userService.resetPassword(verificationToken.getUserId(), newPassword);

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
     * Create a verification token.
     *
     * @param userId the user ID
     * @param tokenType the token type
     * @return the raw token
     */
    private String createToken(UUID userId, VerificationToken.TokenType tokenType) {
        // Generate a secure random token
        String tokenValue = UUID.randomUUID().toString();

        // Hash token for storage
        String tokenHash = TokenHashUtil.hashToken(tokenValue);

        // Calculate expiry
        Instant expiryDate = Instant.now().plus(Duration.ofMinutes(tokenValidityMinutes));

        // Create and save token
        VerificationToken verificationToken = VerificationToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .tokenType(tokenType)
                .expiresAt(expiryDate)
                .used(false)
                .build();

        verificationTokenRepository.save(verificationToken);

        return tokenValue;
    }

    /**
     * Verify a token.
     *
     * @param token the raw token
     * @param tokenType the expected token type
     * @return the verification token
     */
    private VerificationToken verifyToken(String token, VerificationToken.TokenType tokenType) {
        // Hash token for lookup
        String tokenHash = TokenHashUtil.hashToken(token);

        // Find token
        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        // Check token type
        if (verificationToken.getTokenType() != tokenType) {
            throw new InvalidTokenException("Invalid token type");
        }

        // Check if token is expired
        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token has expired");
        }

        // Check if token has been used
        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token has already been used");
        }

        return verificationToken;
    }

    /**
     * Clean up expired and used tokens.
     *
     * @return the number of tokens deleted
     */
    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        return verificationTokenRepository.deleteExpiredTokens(Instant.now());
    }
}