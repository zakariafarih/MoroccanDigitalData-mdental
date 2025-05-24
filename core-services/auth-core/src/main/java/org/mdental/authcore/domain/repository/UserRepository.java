package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Find a user by username and tenant ID.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    /**
     * Find a user by email and tenant ID.
     *
     * @param email the email
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Check if a user exists with the given username and tenant ID.
     *
     * @param username the username
     * @param tenantId the tenant ID
     * @return true if the user exists
     */
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    /**
     * Check if a user exists with the given email and tenant ID.
     *
     * @param email the email
     * @param tenantId the tenant ID
     * @return true if the user exists
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Find a non-deleted user by email and tenant ID.
     *
     * @param email the email
     * @param tenantId the tenant ID
     * @return the user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmailAndTenantId(String email, UUID tenantId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.tenantId = :tenantId")
    List<User> findByTenantIdWithRoles(@Param("tenantId") UUID tenantId);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}