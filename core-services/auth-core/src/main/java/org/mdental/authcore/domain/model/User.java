package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.mdental.commons.model.BaseEntity;
import org.mdental.commons.model.Role;

import java.time.Instant;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;

/**
 * Represents a user in the system.
 * Each user belongs to a specific tenant.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "username"}),
        @UniqueConstraint(columnNames = {"tenant_id", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :clinicId")
public class User extends BaseEntity {
    /**
     * Tenant ID that this user belongs to.
     * Used for multi-tenancy isolation.
     */
   @Column(name = "tenant_id", nullable = false)
   @JoinColumn(foreignKey = @ForeignKey(name = "fk_user_tenant"))
    private java.util.UUID tenantId;

    /**
     * Username, unique within tenant scope.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Email address, unique within tenant scope.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password value.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * User's first name.
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /**
     * Flag indicating if email has been verified.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /**
     * Flag indicating if account is locked.
     */
    @Column(nullable = false)
    private boolean locked;

    /**
     * Timestamp of last successful login.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Roles associated with this user.
     */
    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
}