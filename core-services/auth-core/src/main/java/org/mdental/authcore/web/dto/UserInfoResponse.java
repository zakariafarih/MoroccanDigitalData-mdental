package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.mdental.commons.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for user information.
 */
@Schema(description = "User information")
public record UserInfoResponse(
        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Tenant ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID tenantId,

        @Schema(description = "Username", example = "john.smith")
        String username,

        @Schema(description = "Email address", example = "john.smith@example.com")
        String email,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Smith")
        String lastName,

        @Schema(description = "Whether email is verified")
        boolean emailVerified,

        @Schema(description = "User roles")
        Set<Role> roles,

        @Schema(description = "Last login timestamp")
        Instant lastLoginAt
) {}