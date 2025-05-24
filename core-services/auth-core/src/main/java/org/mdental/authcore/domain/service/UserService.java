package org.mdental.authcore.domain.service;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.User;

/**
 * Service for user management operations.
 */
public interface UserService {

    /**
     * Create a new user.
     *
     * @param user the user to create
     * @param rawPassword the raw password
     * @return the created user
     */
    User createUser(User user, String rawPassword);

    /**
     * Find a user by ID.
     *
     * @param userId the user ID
     * @return the user
     */
    User getUserById(UUID userId);

    /**
     * Find a user by username and tenant ID.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    /**
     * Find a user by email and tenant ID.
     *
     * @param email the email
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Update user password.
     *
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     * @return the updated user
     */
    User updatePassword(UUID userId, String currentPassword, String newPassword);

    /**
     * Reset user password (admin function or password reset).
     *
     * @param userId the user ID
     * @param newPassword the new password
     * @return the updated user
     */
    User resetPassword(UUID userId, String newPassword);

    /**
     * Lock or unlock a user account.
     *
     * @param userId the user ID
     * @param locked the locked status
     * @return the updated user
     */
    User setUserLocked(UUID userId, boolean locked);

    /**
     * Update user profile.
     *
     * @param userId the user ID
     * @param firstName the new first name (or null to keep current)
     * @param lastName the new last name (or null to keep current)
     * @param email the new email (or null to keep current)
     * @return the updated user
     */
    User updateProfile(UUID userId, String firstName, String lastName, String email);

    /**
     * Mark user email as verified.
     *
     * @param userId the user ID
     * @return the updated user
     */
    User verifyEmail(UUID userId);

    /**
     * Record successful login for a user.
     *
     * @param userId the user ID
     */
    void recordLogin(UUID userId);

    /**
     * Verify a user's password.
     *
     * @param user the user
     * @param password the password to verify
     * @return true if the password is valid
     */
    boolean verifyPassword(User user, String password);
}