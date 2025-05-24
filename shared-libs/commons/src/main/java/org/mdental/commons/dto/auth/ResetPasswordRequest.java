package org.mdental.commons.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**

 Request DTO for password reset functionality.
 */
@Schema(description = "Reset password request")
public record ResetPasswordRequest(
        @Schema(description = "Reset token from email")
        @NotBlank(message = "Reset token is required")
        String token,

        @Schema(description = "New password")
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword,

        @Schema(description = "Confirm new password")
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {}