package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new tenant.
 */
@Schema(description = "Request to create a new tenant")
public record CreateTenantRequest(
        @Schema(description = "Tenant slug (unique identifier)", example = "clinic-nyc")
        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
        String slug,

        @Schema(description = "Tenant display name", example = "NYC Dental Clinic")
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name
) {}