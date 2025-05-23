package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to update user locked status.
 */
@Schema(description = "User locked status request")
public record UserLockedRequest(
        @Schema(description = "Whether the user should be locked", example = "true")
        boolean locked
) {}