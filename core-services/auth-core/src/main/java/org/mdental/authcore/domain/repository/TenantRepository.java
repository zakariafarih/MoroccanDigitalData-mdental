package org.mdental.authcore.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.Tenant;

/**
 * Repository port for tenant operations.
 */
public interface TenantRepository {
    Tenant save(Tenant tenant);

    Optional<Tenant> findById(UUID id);

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);
}