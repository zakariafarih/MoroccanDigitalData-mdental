package org.mdental.cliniccore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClinicRequest {

    @NotBlank(message = "Clinic name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 2, max = 50, message = "Slug must be between 2 and 50 characters")
    private String slug;

    @NotBlank(message = "Realm name is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Realm must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 2, max = 50, message = "Realm must be between 2 and 50 characters")
    private String realm;

    private String legalName;
    private String taxId;
    private String description;

    // Branding
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;

    // Compliance
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String privacyPolicyUrl;

    // Defaults
    private String defaultTimeZone;
    private String defaultCurrency;
    private String locale;
}