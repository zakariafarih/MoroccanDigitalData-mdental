package org.mdental.commons.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable record representing authenticated user details extracted from JWT claims
 */
@Schema(description = "Authenticated user principal information")
public record AuthPrincipal(
        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Tenant ID (typically clinic ID)", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID tenantId,

        @Schema(description = "Username", example = "doctor.smith")
        String username,

        @Schema(description = "Email address", example = "doctor.smith@mdental.org")
        String email,

        @Schema(description = "User roles", example = "[\"DOCTOR\", \"CLINIC_ADMIN\"]")
        Set<Role> roles
) {
    /**
     * Creates a new AuthPrincipal instance with empty roles
     */
    public static AuthPrincipal of(UUID id, UUID tenantId, String username, String email) {
        return new AuthPrincipal(id, tenantId, username, email, Collections.emptySet());
    }

    /**
     * Checks if the principal has a specific role
     */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /**
     * Checks if the principal has any of the specified roles
     */
    public boolean hasAnyRole(Role... requiredRoles) {
        for (Role role : requiredRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}