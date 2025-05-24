package org.mdental.commons.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**

 Request DTO for user registration.
 */
@Schema(description = "User registration request")
public record RegisterRequest(
        @Schema(description = "Username", example = "doctor.smith")
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username must be 3-50 characters and may only contain letters, numbers, dots, underscores, and hyphens")
        String username,

        @Schema(description = "Email address", example = "doctor.smith@mdental.org")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "First name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Last name", example = "Smith")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Password")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @Schema(description = "Tenant ID (clinic ID)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID tenantId
) {}