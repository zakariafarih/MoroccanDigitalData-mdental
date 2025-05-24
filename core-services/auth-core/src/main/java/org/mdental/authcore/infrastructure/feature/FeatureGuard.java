package org.mdental.authcore.infrastructure.feature;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.application.service.FeatureService;
import org.mdental.authcore.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Guards access to features.
 */
@Component("featureGuard")
@RequiredArgsConstructor
@Slf4j
public class FeatureGuard {
    private final FeatureService featureService;

    /**
     * Check if the current tenant has access to a feature.
     *
     * @param feature the feature name
     * @return true if the feature is enabled
     */
    public boolean has(String feature) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("No tenant context found when checking feature guard for {}", feature);
            return false;
        }

        return featureService.isFeatureEnabled(tenantId, feature);
    }

    /**
     * Check if a tenant has access to a feature.
     *
     * @param tenantId the tenant ID
     * @param feature the feature name
     * @return true if the feature is enabled
     */
    public boolean has(UUID tenantId, String feature) {
        return featureService.isFeatureEnabled(tenantId, feature);
    }
}