package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing password.
 */
@Schema(description = "Change password request")
public record ChangePasswordRequest(
        @Schema(description = "Current password", example = "OldP@ssw0rd")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "New password", example = "NewP@ssw0rd")
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {}