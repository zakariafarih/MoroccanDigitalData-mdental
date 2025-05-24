package org.mdental.authcore.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.mdental.commons.model.BaseEntity;

import java.util.UUID;

/**
 * Represents a feature flag for a tenant.
 */
@Entity
@Table(name = "tenant_features", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "feature_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeature extends BaseEntity {

    /**
     * The tenant ID.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * The feature name.
     */
    @Column(name = "feature_name", nullable = false)
    private String featureName;

    /**
     * Whether the feature is enabled.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Feature configuration (JSON).
     */
    @Column(name = "config", columnDefinition = "jsonb")
    private String config;
}