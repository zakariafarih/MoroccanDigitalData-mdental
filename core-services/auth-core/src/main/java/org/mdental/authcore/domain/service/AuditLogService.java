package org.mdental.authcore.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.AuditLog;
import org.mdental.authcore.domain.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Service for logging security audit events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an audit event.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID (can be null)
     * @param eventType the event type
     * @param details additional details about the event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID tenantId, UUID userId, AuditLog.EventType eventType, Map<String, Object> details) {
        try {
            String ipAddress = extractIpAddress();
            String requestId = extractRequestId();

            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .eventType(eventType)
                    .ipAddress(ipAddress)
                    .requestId(requestId)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Overloaded method without details.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID tenantId, UUID userId, AuditLog.EventType eventType) {
        log(tenantId, userId, eventType, Collections.emptyMap());
    }

    /**
     * Find audit logs for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByTenant(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    /**
     * Find audit logs for a user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Extract the client IP address from the current request.
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String xff = attributes.getRequest().getHeader("X-Forwarded-For");
                if (xff != null && !xff.isEmpty()) {
                    return xff.split(",")[0].trim();
                }
                return attributes.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Failed to extract IP address", e);
        }
        return null;
    }

    /**
     * Extract the request ID from the current request.
     */
    private String extractRequestId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("X-Request-ID");
            }
        } catch (Exception e) {
            log.warn("Failed to extract request ID", e);
        }
        return null;
    }
}