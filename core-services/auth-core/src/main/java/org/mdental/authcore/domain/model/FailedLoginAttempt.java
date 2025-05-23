package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.mdental.commons.model.BaseEntity;

import java.time.Instant;

/**
 * Records failed login attempts for security monitoring and account lockout.
 */
@Entity
@Table(name = "failed_login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedLoginAttempt extends BaseEntity {
    /**
     * Username that was used in the failed attempt
     */
    @Column(nullable = false)
    private String username;

    /**
     * Tenant ID where the login was attempted
     */
    @Column(name = "tenant_id", nullable = false)
    private java.util.UUID tenantId;

    /**
     * IP address of the client that attempted the login
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * Timestamp when the attempt occurred
     */
    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}