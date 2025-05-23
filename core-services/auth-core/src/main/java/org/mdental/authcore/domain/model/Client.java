package org.mdental.authcore.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mdental.commons.model.BaseEntity;
import org.mdental.commons.model.Role;

/**
 * Represents an OAuth client (service account).
 */
@Entity
@Table(name = "oauth_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client extends BaseEntity {

    /**
     * The client ID (public identifier).
     */
    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    /**
     * The hashed client secret.
     */
    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    /**
     * The client name.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Whether the client is active.
     */
    @Column(name = "active", nullable = false)
    private boolean active;

    /**
     * The tenant ID (null for multi-tenant clients).
     */
    @Column(name = "tenant_id")
    private java.util.UUID tenantId;

    /**
     * Allowed grant types.
     */
    @ElementCollection
    @CollectionTable(name = "oauth_client_grant_types", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type")
    private Set<String> authorizedGrantTypes;

    /**
     * The client roles.
     */
    @ElementCollection
    @CollectionTable(name = "oauth_client_roles", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    /**
     * Token validity in seconds.
     */
    @Column(name = "access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;
}