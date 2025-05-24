package org.mdental.authcore.domain.event;

/**
 * Email-related events.
 */
public enum EmailEvent {
    /**
     * Email requested to be sent.
     */
    EMAIL_REQUESTED,

    /**
     * Email sent successfully.
     */
    EMAIL_SENT,

    /**
     * Email sending failed.
     */
    EMAIL_FAILED
}