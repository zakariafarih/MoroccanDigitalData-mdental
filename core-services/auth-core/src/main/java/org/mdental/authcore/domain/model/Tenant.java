package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.mdental.commons.model.BaseEntity;

import java.util.Set;

/**
 * Represents a tenant in the system (clinic).
 * Each tenant is isolated and has its own users.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {
    /**
     * Unique slug identifier for the tenant
     */
    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * Display name of the tenant
     */
    @Column(nullable = false)
    private String name;

    /**
     * Flag indicating if the tenant is active
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Users belonging to this tenant
     */
    @OneToMany(mappedBy = "tenantId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<User> users;
}