package org.mdental.authcore.infrastructure.jpa;

import java.time.Instant;
import java.util.UUID;
import org.mdental.authcore.domain.model.FailedLoginAttempt;
import org.mdental.authcore.domain.repository.FailedLoginAttemptRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedLoginAttemptRepositoryImpl extends FailedLoginAttemptRepository, JpaRepository<FailedLoginAttempt, UUID> {
    @Override
    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE f.username = :username AND f.tenantId = :tenantId AND f.attemptedAt > :since")
    int countRecentFailedAttempts(String username, UUID tenantId, Instant since);

    @Override
    long deleteByAttemptedAtBefore(Instant before);
}