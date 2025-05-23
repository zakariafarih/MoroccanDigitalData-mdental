package org.mdental.commons.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**

 Request DTO for user login.
 */
@Schema(description = "User login request")
public record LoginRequest(
        @Schema(description = "Username or email address", example = "doctor.smith@mdental.org")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Password")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {}