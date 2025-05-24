package org.mdental.security.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clinicIdHeader = request.getHeader("X-User-ClinicId");
        if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
            try {
                UUID clinicId = UUID.fromString(clinicIdHeader);
                TenantContext.setTenantId(clinicId);

                // Enable Hibernate filter
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("clinicId", clinicId);

                log.debug("Tenant filter enabled for clinic ID: {}", clinicId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid clinic ID format in header: {}", clinicIdHeader);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clear tenant context after request is complete
        TenantContext.clear();
    }
}