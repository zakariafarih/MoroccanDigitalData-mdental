package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
@Schema(description = "Login request")
public record LoginRequest(
        @Schema(description = "Username", example = "john.smith")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Password", example = "SecureP@ssw0rd")
        @NotBlank(message = "Password is required")
        String password
) {}