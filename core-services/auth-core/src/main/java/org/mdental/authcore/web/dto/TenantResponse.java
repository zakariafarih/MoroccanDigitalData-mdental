package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tenant information.
 */
@Schema(description = "Tenant information")
public record TenantResponse(
        @Schema(description = "Tenant ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Tenant slug", example = "clinic-nyc")
        String slug,

        @Schema(description = "Tenant name", example = "NYC Dental Clinic")
        String name,

        @Schema(description = "Whether the tenant is active")
        boolean active,

        @Schema(description = "When the tenant was created")
        Instant createdAt
) {}