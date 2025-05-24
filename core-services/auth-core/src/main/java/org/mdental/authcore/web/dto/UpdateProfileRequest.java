// src/main/java/org/mdental/authcore/web/dto/UpdateProfileRequest.java
package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

/**
 * Request DTO for updating user profile.
 */
@Schema(description = "Update profile request")
public record UpdateProfileRequest(
        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Smith")
        String lastName,

        @Schema(description = "Email address", example = "john.smith@example.com")
        @Email(message = "Email must be valid")
        String email
) {}