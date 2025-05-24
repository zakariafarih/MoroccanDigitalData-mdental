package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new tenant with an admin user.
 */
@Schema(description = "Request to create a new tenant with an admin user")
public record CreateTenantWithAdminRequest(
        @Schema(description = "Tenant slug (unique identifier)", example = "clinic-nyc")
        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
        String slug,

        @Schema(description = "Tenant display name", example = "NYC Dental Clinic")
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Schema(description = "Admin username", example = "admin.nyc")
        @NotBlank(message = "Admin username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username must be 3-50 characters and contain only letters, numbers, dots, underscores, and hyphens")
        String adminUsername,

        @Schema(description = "Admin email", example = "admin@nyc-clinic.com")
        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be valid")
        String adminEmail,

        @Schema(description = "Admin first name", example = "John")
        @NotBlank(message = "Admin first name is required")
        String adminFirstName,

        @Schema(description = "Admin last name", example = "Smith")
        @NotBlank(message = "Admin last name is required")
        String adminLastName
) {}