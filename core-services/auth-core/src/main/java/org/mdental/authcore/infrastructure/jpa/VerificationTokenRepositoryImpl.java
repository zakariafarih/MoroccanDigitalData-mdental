package org.mdental.authcore.infrastructure.jpa;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.VerificationToken;
import org.mdental.authcore.domain.repository.VerificationTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepositoryImpl extends VerificationTokenRepository, JpaRepository<VerificationToken, UUID> {
    @Override
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    @Override
    Optional<VerificationToken> findFirstByUserIdAndTokenTypeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
            UUID userId,
            VerificationToken.TokenType tokenType,
            Instant now
    );

    @Override
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredTokens(Instant now);
}