package org.mdental.authcore.infrastructure.jpa;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.RefreshToken;
import org.mdental.authcore.domain.repository.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepositoryImpl extends RefreshTokenRepository, JpaRepository<RefreshToken, UUID> {
    @Override
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Override
    Optional<RefreshToken> findByPreviousTokenHash(String previousTokenHash);

    @Override
    long countByUserIdAndExpiresAtAfterAndRevokedFalse(UUID userId, Instant now);

    @Override
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    int revokeAllUserTokens(UUID userId);

    @Override
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    int deleteExpiredTokens(Instant now);
}