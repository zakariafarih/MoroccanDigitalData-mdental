package org.mdental.authcore.domain.event;

/**
 * User-related events.
 */
public enum UserEvent {
    /**
     * User created.
     */
    CREATED,

    /**
     * User profile updated.
     */
    PROFILE_UPDATED,

    /**
     * User password changed.
     */
    PASSWORD_CHANGED,

    /**
     * User password reset.
     */
    PASSWORD_RESET,

    /**
     * User password reset requested.
     */
    PASSWORD_RESET_REQUESTED,

    /**
     * User password reset completed.
     */
    PASSWORD_RESET_COMPLETED,

    /**
     * User email changed.
     */
    EMAIL_CHANGED,

    /**
     * User email verified.
     */
    EMAIL_VERIFIED,

    /**
     * User account locked.
     */
    LOCKED,

    /**
     * User account unlocked.
     */
    UNLOCKED
}
