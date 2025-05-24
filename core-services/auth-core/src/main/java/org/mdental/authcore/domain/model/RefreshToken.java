package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.mdental.commons.model.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a refresh token for renewing access tokens.
 * Only the hash of the token is stored for security.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {
    /**
     * User ID that this token belongs to.
     */
    @Column(name = "user_id", nullable = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    private UUID userId;

    /**
     * Tenant ID that this token belongs to.
     */
    @Column(name = "tenant_id", nullable = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_refresh_token_tenant"))
    private UUID tenantId;

    /**
     * SHA-256 hash of the refresh token.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Expiration timestamp of the token.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Flag indicating if this token has been revoked.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Previous token hash that this token replaced (for rotation tracking).
     */
    @Column(name = "previous_token_hash", length = 64)
    private String previousTokenHash;
}