package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
@Schema(description = "User registration request")
public record RegisterRequest(
        @Schema(description = "Username", example = "john.smith")
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username must be 3-50 characters and contain only letters, numbers, dots, underscores, and hyphens")
        String username,

        @Schema(description = "Email address", example = "john.smith@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "First name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Last name", example = "Smith")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Password", example = "SecureP@ssw0rd")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Role (optional, defaults to PATIENT)", example = "DOCTOR")
        String role
) {}