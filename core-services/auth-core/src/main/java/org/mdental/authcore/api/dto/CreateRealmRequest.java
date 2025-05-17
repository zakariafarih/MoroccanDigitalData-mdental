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
public class CreateRealmRequest {

    @NotBlank(message = "Clinic slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Clinic slug must contain only lowercase letters, numbers, and hyphens")
    private String clinicSlug;

    @NotBlank(message = "Realm name is required")
    @Pattern(regexp = "^mdental-[a-z0-9-]+$", message = "Realm name must start with 'mdental-' followed by lowercase letters, numbers, and hyphens")
    private String realm;
}