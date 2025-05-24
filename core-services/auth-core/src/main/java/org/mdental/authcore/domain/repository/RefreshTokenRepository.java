package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for refresh token operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Find a refresh token by its hash.
     *
     * @param tokenHash the token hash
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find a refresh token by its previous hash (for token rotation).
     *
     * @param previousTokenHash the previous token hash
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByPreviousTokenHash(String previousTokenHash);

    /**
     * Find valid refresh tokens for a specific user.
     *
     * @param userId the user ID
     * @param now the current timestamp
     * @return the count of valid tokens
     */
    long countByUserIdAndExpiresAtAfterAndRevokedFalse(UUID userId, Instant now);

    /**
     * Revoke all refresh tokens for a specific user.
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    int revokeAllUserTokens(UUID userId);

    /**
     * Delete expired tokens.
     *
     * @param now the current timestamp
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    int deleteExpiredTokens(Instant now);
}
