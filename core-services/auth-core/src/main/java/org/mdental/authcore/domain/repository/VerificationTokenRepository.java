package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for verification token operations.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    /**
     * Find a token by its hash.
     *
     * @param tokenHash the token hash
     * @return the token if found
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Find the latest valid token for a user and type.
     *
     * @param userId the user ID
     * @param tokenType the token type
     * @param now the current timestamp
     * @return the token if found
     */
    Optional<VerificationToken> findFirstByUserIdAndTokenTypeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
            UUID userId,
            VerificationToken.TokenType tokenType,
            Instant now
    );

    /**
     * Delete expired tokens.
     *
     * @param now the current timestamp
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredTokens(Instant now);
}