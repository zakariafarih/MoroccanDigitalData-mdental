package org.mdental.authcore.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.TenantFeature;

/**
 * Repository port for tenant feature operations.
 */
public interface TenantFeatureRepository {
    /**
     * Find a tenant feature by tenant ID and feature name.
     *
     * @param tenantId the tenant ID
     * @param featureName the feature name
     * @return the tenant feature if found
     */
    Optional<TenantFeature> findByTenantIdAndFeatureName(UUID tenantId, String featureName);

    /**
     * Find all tenant features for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the tenant features
     */
    List<TenantFeature> findAllByTenantId(UUID tenantId);

    /**
     * Save a tenant feature.
     *
     * @param tenantFeature the tenant feature to save
     * @return the saved tenant feature
     */
    TenantFeature save(TenantFeature tenantFeature);
}