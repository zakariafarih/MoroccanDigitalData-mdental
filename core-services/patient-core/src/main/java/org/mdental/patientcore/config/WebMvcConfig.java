package org.mdental.patientcore.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.mdental.patientcore.exception.ValidationException;
import org.mdental.patientcore.util.TenantContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor());
    }

    class TenantInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String tenantHeader = request.getHeader("X-User-ClinicId");
            String pathClinicId = extractClinicIdFromPath(request.getRequestURI());
            String requestId = request.getHeader("X-Gateway-Request-Id");

            // Add request ID to MDC for logging
            if (requestId != null) {
                MDC.put("requestId", requestId);
            }

            if (isSuperAdmin()) {
                // Super admins can access any clinic's data
                setTenantId(pathClinicId != null ? UUID.fromString(pathClinicId) : null);
                return true;
            }

            // For regular users, verify clinic ID in header matches the one in path
            if (tenantHeader == null) {
                throw new ValidationException("Missing X-User-ClinicId header");
            }

            if (pathClinicId != null && !pathClinicId.equals(tenantHeader)) {
                throw new ValidationException("Clinic ID in path does not match authenticated clinic");
            }

            setTenantId(UUID.fromString(tenantHeader));
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            // Disable tenant filter
            if (entityManager != null) {
                Session session = entityManager.unwrap(Session.class);
                if (session != null) {
                    session.disableFilter("tenantFilter");
                }
            }

            // Clear tenant context
            TenantContext.clear();

            // Clear MDC
            MDC.clear();
        }

        private String extractClinicIdFromPath(String uri) {
            if (uri == null) return null;

            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("clinics") && i+1 < parts.length) {
                    return parts[i+1];
                }
            }
            return null;
        }

        private boolean isSuperAdmin() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return false;

            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            return authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        }

        private void setTenantId(UUID clinicId) {
            TenantContext.setCurrentTenant(clinicId);

            if (clinicId != null) {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("clinicId", clinicId);
            }
        }
    }
}