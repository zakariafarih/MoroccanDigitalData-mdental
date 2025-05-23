package org.mdental.authcore.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.TenantFeature;
import org.mdental.authcore.domain.repository.TenantFeatureRepository;
import org.mdental.authcore.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for feature flag operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {
    private final TenantFeatureRepository tenantFeatureRepository;
    private final ObjectMapper objectMapper;

    /**
     * Check if a feature is enabled for the current tenant.
     *
     * @param featureName the feature name
     * @return true if the feature is enabled
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureName) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("No tenant context found when checking feature {}", featureName);
            return false;
        }

        return isFeatureEnabled(tenantId, featureName);
    }

    /**
     * Check if a feature is enabled for a tenant.
     *
     * @param tenantId the tenant ID
     * @param featureName the feature name
     * @return true if the feature is enabled
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(UUID tenantId, String featureName) {
        return tenantFeatureRepository.findByTenantIdAndFeatureName(tenantId, featureName)
                .map(TenantFeature::isEnabled)
                .orElse(false);
    }

    /**
     * Get the configuration for a feature.
     *
     * @param featureName the feature name
     * @return the configuration or an empty map if the feature is not found
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeatureConfig(String featureName) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("No tenant context found when getting feature config {}", featureName);
            return new HashMap<>();
        }

        return getFeatureConfig(tenantId, featureName);
    }

    /**
     * Get the configuration for a feature.
     *
     * @param tenantId the tenant ID
     * @param featureName the feature name
     * @return the configuration or an empty map if the feature is not found
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeatureConfig(UUID tenantId, String featureName) {
        return tenantFeatureRepository.findByTenantIdAndFeatureName(tenantId, featureName)
                .map(feature -> {
                    try {
                        if (feature.getConfig() != null && !feature.getConfig().isBlank()) {
                            return objectMapper.readValue(feature.getConfig(), Map.class);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing feature config for {}: {}", featureName, e.getMessage(), e);
                    }
                    return new HashMap<String, Object>();
                })
                .orElse(new HashMap<>());
    }

    /**
     * Enable a feature for a tenant.
     *
     * @param tenantId the tenant ID
     * @param featureName the feature name
     * @param config the feature configuration (optional)
     * @return the tenant feature
     */
    @Transactional
    public TenantFeature enableFeature(UUID tenantId, String featureName, Map<String, Object> config) {
        TenantFeature feature = tenantFeatureRepository.findByTenantIdAndFeatureName(tenantId, featureName)
                .orElse(TenantFeature.builder()
                        .tenantId(tenantId)
                        .featureName(featureName)
                        .build());

        feature.setEnabled(true);

        if (config != null && !config.isEmpty()) {
            try {
                feature.setConfig(objectMapper.writeValueAsString(config));
            } catch (Exception e) {
                log.error("Error serializing feature config for {}: {}", featureName, e.getMessage(), e);
            }
        }

        return tenantFeatureRepository.save(feature);
    }

    /**
     * Disable a feature for a tenant.
     *
     * @param tenantId the tenant ID
     * @param featureName the feature name
     */
    @Transactional
    public void disableFeature(UUID tenantId, String featureName) {
        tenantFeatureRepository.findByTenantIdAndFeatureName(tenantId, featureName)
                .ifPresent(feature -> {
                    feature.setEnabled(false);
                    tenantFeatureRepository.save(feature);
                });
    }
}