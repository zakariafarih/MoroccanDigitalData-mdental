package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs for a specific tenant.
     */
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Find audit logs for a specific user.
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find audit logs for a specific tenant and event type.
     */
    Page<AuditLog> findByTenantIdAndEventTypeOrderByCreatedAtDesc(
            UUID tenantId, AuditLog.EventType eventType, Pageable pageable);

    /**
     * Find recent audit logs for a specific tenant.
     */
    List<AuditLog> findByTenantIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID tenantId, Instant since, Pageable pageable);
}