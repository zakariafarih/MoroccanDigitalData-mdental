package org.mdental.security.tenant;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.mdental.commons.constants.MdentalHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter {

    private final EntityManager entityManager;

    public void activateFilter() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter").setParameter("clinicId", tenantId);
            }
        }
    }

    public void initTenantContext() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                String clinicIdStr = attributes.getRequest().getHeader(MdentalHeaders.USER_CLINIC_ID);
                if (clinicIdStr != null && !clinicIdStr.isBlank()) {
                    try {
                        UUID clinicId = UUID.fromString(clinicIdStr);
                        TenantContext.setTenantId(clinicId);
                    } catch (IllegalArgumentException e) {
                        // Invalid UUID format, don't set tenant ID
                    }
                }
            }
        } catch (Exception e) {
            // If request context isn't available, continue without tenant context
        }
    }
}