package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.mdental.commons.model.BaseEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing security audit logs.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    public enum EventType {
        // Authentication events
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGIN_LOCKOUT,
        LOGIN_ATTEMPT_LOCKED_ACCOUNT,
        LOGIN_ATTEMPT_UNVERIFIED_EMAIL,
        LOGOUT,

        // Token events
        TOKEN_CREATED,
        TOKEN_REFRESHED,
        TOKEN_EXPIRED,
        TOKEN_REVOKED,
        TOKEN_REPLAY_ATTACK,
        ALL_TOKENS_REVOKED,

        // User events
        USER_CREATED,
        USER_UPDATED,
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_COMPLETED,
        EMAIL_VERIFIED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,

        // Tenant events
        TENANT_CREATED,
        TENANT_UPDATED
    }

    /**
     * The tenant ID associated with this event.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * The user ID associated with this event (if applicable).
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * The type of event.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    /**
     * IP address of the client that performed the action.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * Request ID for correlation.
     */
    @Column(name = "request_id")
    private String requestId;

    /**
     * Additional event details as JSON.
     */
    @Column(name = "details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;
}