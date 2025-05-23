package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for password reset.
 */
@Schema(description = "Reset password request")
public record ResetPasswordRequest(
        @Schema(description = "Reset token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "New password", example = "NewP@ssw0rd")
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {}