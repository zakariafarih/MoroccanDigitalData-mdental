package org.mdental.authcore.domain.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.UserEvent;
import org.mdental.authcore.persistence.entity.User;
import org.mdental.authcore.persistence.repository.UserRepository;
import org.mdental.authcore.exception.AuthenticationException;
import org.mdental.authcore.exception.DuplicateResourceException;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.exception.ValidationException;
import org.mdental.authcore.util.PasswordPolicy;
import org.mdental.security.password.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management operations.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final OutboxService outboxService;
    private final PasswordPolicy passwordPolicy;

    /**
     * Create a new user.
     *
     * @param user the user to create
     * @param rawPassword the raw password
     * @return the created user
     */
    @Transactional
    public User createUser(@Valid @NotNull User user, @NotNull String rawPassword) {
        log.info("Creating new user: {} for tenant: {}", user.getUsername(), user.getTenantId());

        // Validate user data
        validateNewUser(user);

        // Validate password
        passwordPolicy.validate(rawPassword);

        // Hash password
        user.setPasswordHash(passwordService.hash(rawPassword.toCharArray()));

        // Save user
        User savedUser = userRepository.save(user);

        // Publish user created event
        outboxService.saveEvent(
                "User",
                savedUser.getId(),
                UserEvent.CREATED.name(),
                null,
                savedUser
        );

        return savedUser;
    }

    /**
     * Find a user by ID.
     *
     * @param userId the user ID
     * @return the user
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
    }

    /**
     * Find a user by username and tenant ID.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @return the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameAndTenantId(String username, UUID tenantId) {
        return userRepository.findByUsernameAndTenantId(username, tenantId);
    }

    /**
     * Find a user by email and tenant ID.
     *
     * @param email the email
     * @param tenantId the tenant ID
     * @return the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailAndTenantId(String email, UUID tenantId) {
        return userRepository.findByEmailAndTenantId(email, tenantId);
    }

    /**
     * Update user password.
     *
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     * @return the updated user
     */
    @Transactional
    public User updatePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        // Verify current password
        if (!passwordService.matches(currentPassword.toCharArray(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Validate new password
        passwordPolicy.validate(newPassword);

        // Hash new password
        user.setPasswordHash(passwordService.hash(newPassword.toCharArray()));

        User updatedUser = userRepository.save(user);

        // Publish password changed event
        outboxService.saveEvent(
                "User",
                user.getId(),
                UserEvent.PASSWORD_CHANGED.name(),
                null,
                user.getId()
        );

        return updatedUser;
    }

    /**
     * Reset user password (admin function or password reset).
     *
     * @param userId the user ID
     * @param newPassword the new password
     * @return the updated user
     */
    @Transactional
    public User resetPassword(UUID userId, String newPassword) {
        User user = getUserById(userId);

        // Validate new password
        passwordPolicy.validate(newPassword);

        // Hash new password
        user.setPasswordHash(passwordService.hash(newPassword.toCharArray()));

        User updatedUser = userRepository.save(user);

        // Publish password reset event
        outboxService.saveEvent(
                "User",
                user.getId(),
                UserEvent.PASSWORD_RESET.name(),
                null,
                user.getId()
        );

        return updatedUser;
    }

    /**
     * Lock or unlock a user account.
     *
     * @param userId the user ID
     * @param locked the locked status
     * @return the updated user
     */
    @Transactional
    public User setUserLocked(UUID userId, boolean locked) {
        User user = getUserById(userId);

        if (user.isLocked() != locked) {
            user.setLocked(locked);
            user = userRepository.save(user);

            // Publish user locked/unlocked event
            UserEvent event = locked ? UserEvent.LOCKED : UserEvent.UNLOCKED;
            outboxService.saveEvent(
                    "User",
                    user.getId(),
                    event.name(),
                    null,
                    user.getId()
            );
        }

        return user;
    }

    /**
     * Update user profile.
     *
     * @param userId the user ID
     * @param firstName the new first name (or null to keep current)
     * @param lastName the new last name (or null to keep current)
     * @param email the new email (or null to keep current)
     * @return the updated user
     */
    @Transactional
    public User updateProfile(UUID userId, String firstName, String lastName, String email) {
        User user = getUserById(userId);
        boolean changed = false;
        boolean emailChanged = false;

        if (firstName != null && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            changed = true;
        }

        if (lastName != null && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            changed = true;
        }

        if (email != null && !email.equals(user.getEmail())) {
            // Check if email is already in use
            if (userRepository.existsByEmailAndTenantId(email, user.getTenantId())) {
                throw new DuplicateResourceException("Email already in use");
            }

            user.setEmail(email);
            user.setEmailVerified(false); // Require verification of new email
            changed = true;
            emailChanged = true;
        }

        if (changed) {
            user = userRepository.save(user);

            // Publish profile updated event
            outboxService.saveEvent(
                    "User",
                    user.getId(),
                    UserEvent.PROFILE_UPDATED.name(),
                    null,
                    user
            );

            // Publish email changed event if needed
            if (emailChanged) {
                outboxService.saveEvent(
                        "User",
                        user.getId(),
                        UserEvent.EMAIL_CHANGED.name(),
                        null,
                        user
                );
            }
        }

        return user;
    }

    /**
     * Mark user email as verified.
     *
     * @param userId the user ID
     * @return the updated user
     */
    @Transactional
    public User verifyEmail(UUID userId) {
        User user = getUserById(userId);

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user = userRepository.save(user);

            // Publish email verified event
            outboxService.saveEvent(
                    "User",
                    user.getId(),
                    UserEvent.EMAIL_VERIFIED.name(),
                    null,
                    user.getId()
            );
        }

        return user;
    }

    /**
     * Record successful login for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void recordLogin(UUID userId) {
        User user = getUserById(userId);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * Validate a new user.
     *
     * @param user the user to validate
     * @throws ValidationException if the user is invalid
     */
    private void validateNewUser(User user) {
        // Check username
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ValidationException("Username cannot be empty");
        }

        // Check email
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be empty");
        }

        // Check for duplicate username
        if (userRepository.existsByUsernameAndTenantId(user.getUsername(), user.getTenantId())) {
            throw new DuplicateResourceException("Username already exists in this tenant");
        }

        // Check for duplicate email
        if (userRepository.existsByEmailAndTenantId(user.getEmail(), user.getTenantId())) {
            throw new DuplicateResourceException("Email already exists in this tenant");
        }
    }
}
