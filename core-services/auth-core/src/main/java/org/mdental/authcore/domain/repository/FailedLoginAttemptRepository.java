package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for failed login attempt operations.
 */
@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {
    /**
     * Count recent failed login attempts for a username in a tenant.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @param since the time threshold
     * @return the count of failed attempts
     */
    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE f.username = :username AND f.tenantId = :tenantId AND f.attemptedAt > :since")
    int countRecentFailedAttempts(String username, UUID tenantId, Instant since);

    /**
     * Delete old login attempts.
     *
     * @param before the time threshold
     * @return the number of records deleted
     */
    long deleteByAttemptedAtBefore(Instant before);
}