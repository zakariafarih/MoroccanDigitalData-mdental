package org.mdental.authcore.domain.event;

/**
 * Authentication events.
 */
public enum AuthEvent {
    /**
     * Successful login.
     */
    LOGIN_SUCCESS,

    /**
     * Failed login attempt.
     */
    LOGIN_FAILED,

    /**
     * Token refresh.
     */
    TOKEN_REFRESHED,

    /**
     * Token revoked (logout).
     */
    TOKEN_REVOKED
}
