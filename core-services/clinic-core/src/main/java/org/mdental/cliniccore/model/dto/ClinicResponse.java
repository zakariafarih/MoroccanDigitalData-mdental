package org.mdental.cliniccore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mdental.cliniccore.model.entity.Clinic;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicResponse {

    private UUID id;
    private String name;
    private String slug;
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

    private Clinic.ClinicStatus status;

    private Set<ContactInfoResponse> contactInfos;
    private Set<AddressResponse> addresses;
    private Set<BusinessHoursResponse> businessHours;
    private Set<HolidayResponse> holidays;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}