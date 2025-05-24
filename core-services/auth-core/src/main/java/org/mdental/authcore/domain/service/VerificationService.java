package org.mdental.authcore.domain.service;

import org.mdental.authcore.domain.model.User;

/**
 * Service for handling verification tokens.
 */
public interface VerificationService {
    /**
     * Create email verification token and send email.
     *
     * @param user the user
     * @return the token
     */
    String createEmailVerificationToken(User user);

    /**
     * Create password reset token and send email.
     *
     * @param user the user
     * @return the token
     */
    String createPasswordResetToken(User user);

    /**
     * Verify email using token.
     *
     * @param token the verification token
     * @return the user
     */
    User verifyEmail(String token);

    /**
     * Reset password using token.
     *
     * @param token the reset token
     * @param newPassword the new password
     * @return the user
     */
    User resetPassword(String token, String newPassword);

    /**
     * Clean up expired and used tokens.
     *
     * @return the number of tokens deleted
     */
    int cleanupExpiredTokens();
}