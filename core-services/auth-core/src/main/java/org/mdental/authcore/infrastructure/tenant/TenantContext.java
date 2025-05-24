package org.mdental.authcore.infrastructure.tenant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.NamedThreadLocal;

import java.util.UUID;

/**
 * Thread-local holder for tenant context information.
 */
@Slf4j
public final class TenantContext {
    private static final ThreadLocal<UUID> TENANT_ID = new NamedThreadLocal<>("Tenant ID");
    private static final String MDC_TENANT_KEY = "tenantId";

    private TenantContext() {
        // Utility class
    }

    /**
     * Set the tenant ID for the current thread.
     *
     * @param tenantId the tenant ID
     */
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
        if (tenantId != null) {
            MDC.put(MDC_TENANT_KEY, tenantId.toString());
        } else {
            MDC.remove(MDC_TENANT_KEY);
        }
    }

    /**
     * Get the tenant ID for the current thread.
     *
     * @return the tenant ID or null if not set
     */
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Clear the tenant ID for the current thread.
     */
    public static void clear() {
        TENANT_ID.remove();
        MDC.remove(MDC_TENANT_KEY);
    }
}