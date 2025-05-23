package org.mdental.authcore.infrastructure.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.domain.repository.TenantRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that extracts tenant information from the request and sets up the tenant context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantHeaderFilter extends OncePerRequestFilter {
    private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("/auth/([^/]+).*");

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // First try to extract from path variable
            String tenantSlug = extractTenantFromPath(request.getRequestURI());

            // If not found, try header
            if (tenantSlug == null) {
                tenantSlug = request.getHeader("X-Tenant");
            }

            if (tenantSlug != null && !tenantSlug.isBlank()) {
                tenantRepository.findBySlug(tenantSlug).ifPresent(tenant -> {
                    setTenantContext(tenant);
                });
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extract tenant slug from the request path.
     *
     * @param path the request path
     * @return the tenant slug or null if not found
     */
    private String extractTenantFromPath(String path) {
        Matcher matcher = TENANT_PATH_PATTERN.matcher(path);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Set the tenant context.
     *
     * @param tenant the tenant
     */
    private void setTenantContext(Tenant tenant) {
        if (tenant != null) {
            TenantContext.setTenantId(tenant.getId());
            log.debug("Set tenant context: {} ({})", tenant.getSlug(), tenant.getId());
        }
    }
}