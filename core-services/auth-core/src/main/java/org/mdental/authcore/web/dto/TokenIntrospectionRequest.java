package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for OAuth token introspection.
 */
@Schema(description = "OAuth token introspection request")
public record TokenIntrospectionRequest(
        @Schema(description = "Token to introspect", example = "eyJhb...")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "Token type hint", example = "access_token")
        String tokenTypeHint
) {}