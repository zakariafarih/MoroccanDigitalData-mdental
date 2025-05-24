package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 */
@Schema(description = "Token refresh request")
public record RefreshRequest(
        @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}