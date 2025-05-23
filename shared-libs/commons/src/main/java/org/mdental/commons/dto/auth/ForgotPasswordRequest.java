package org.mdental.commons.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**

 Request DTO for forgot password functionality.
 */
@Schema(description = "Forgot password request")
public record ForgotPasswordRequest(
        @Schema(description = "Email address", example = "doctor.smith@mdental.org")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {}