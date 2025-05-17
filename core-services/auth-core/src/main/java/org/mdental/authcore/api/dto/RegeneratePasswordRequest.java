package org.mdental.authcore.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratePasswordRequest {

    @NotBlank(message = "Admin username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username must contain only letters, numbers, hyphens and underscores")
    private String adminUsername;
}