package org.mdental.authcore.infrastructure.jpa;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepositoryImpl extends UserRepository, JpaRepository<User, UUID> {
    @Override
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    @Override
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    @Override
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    @Override
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    @Override
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmailAndTenantId(String email, UUID tenantId);
}