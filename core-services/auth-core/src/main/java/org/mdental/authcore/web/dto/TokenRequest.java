package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for OAuth token endpoint.
 */
@Schema(description = "OAuth token request")
public record TokenRequest(
        @Schema(description = "Grant type (refresh_token, client_credentials)", example = "refresh_token")
        @NotBlank(message = "Grant type is required")
        String grantType,

        @Schema(description = "Refresh token (required for refresh_token grant type)", example = "eyJhb...")
        String refreshToken,

        @Schema(description = "Client ID (required for client_credentials grant type)", example = "api-gateway")
        String clientId,

        @Schema(description = "Client secret (required for client_credentials grant type)")
        String clientSecret
) {}