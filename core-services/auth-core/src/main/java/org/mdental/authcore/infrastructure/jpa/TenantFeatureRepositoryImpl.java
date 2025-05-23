package org.mdental.authcore.infrastructure.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.TenantFeature;
import org.mdental.authcore.domain.repository.TenantFeatureRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantFeatureRepositoryImpl extends TenantFeatureRepository, JpaRepository<TenantFeature, UUID> {
    @Override
    Optional<TenantFeature> findByTenantIdAndFeatureName(UUID tenantId, String featureName);

    @Override
    List<TenantFeature> findAllByTenantId(UUID tenantId);
}