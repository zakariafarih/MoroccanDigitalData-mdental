package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for forgotten password.
 */
@Schema(description = "Forgot password request")
public record ForgotPasswordRequest(
        @Schema(description = "Email address", example = "john.smith@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {}