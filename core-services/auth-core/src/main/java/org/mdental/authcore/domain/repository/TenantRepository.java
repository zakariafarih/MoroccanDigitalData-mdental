package org.mdental.authcore.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository port for tenant operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
}