package org.mdental.authcore.infrastructure.jpa;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.domain.repository.TenantRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepositoryImpl extends TenantRepository, JpaRepository<Tenant, UUID> {
    @Override
    Optional<Tenant> findBySlug(String slug);

    @Override
    boolean existsBySlug(String slug);
}