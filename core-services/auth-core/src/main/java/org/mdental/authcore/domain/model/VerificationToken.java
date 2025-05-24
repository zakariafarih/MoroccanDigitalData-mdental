package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.mdental.commons.model.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Token for email verification or password reset.
 */
@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken extends BaseEntity {

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    /**
     * User ID that this token belongs to.
     */
    @Column(name = "user_id", nullable = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_verification_token_user"))
    private UUID userId;

    /**
     * Token value (hashed for security).
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Type of token.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    /**
     * Expiration timestamp of the token.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Flag indicating if this token has been used.
     */
    @Column(nullable = false)
    private boolean used;
}