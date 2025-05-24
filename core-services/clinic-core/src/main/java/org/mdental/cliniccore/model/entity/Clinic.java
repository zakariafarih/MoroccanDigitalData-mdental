package org.mdental.cliniccore.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clinics", indexes = {
        @Index(name = "idx_clinic_realm", columnList = "realm"),
        @Index(name = "idx_clinic_slug", columnList = "slug"),
        @Index(name = "idx_clinic_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Clinic extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "description", length = 2000)
    private String description;

    // Branding
    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    // Compliance
    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "privacy_policy_url")
    private String privacyPolicyUrl;

    // Defaults
    @Column(name = "default_time_zone")
    private String defaultTimeZone;

    @Column(name = "default_currency")
    private String defaultCurrency;

    @Column(name = "locale")
    private String locale;

    @Column(name = "realm", nullable = false, unique = true)
    private String realm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClinicStatus status;

    // Relationships
    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContactInfo> contactInfos = new HashSet<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<BusinessHours> businessHours = new HashSet<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Holiday> holidays = new HashSet<>();

    @Column(name = "kc_issuer")
    private String kcIssuer;

    @Column(name = "kc_admin_user")
    private String kcAdminUser;

    @Column(name = "kc_tmp_password")
    private String kcTmpPassword;

    // Helper methods for managing relationships
    public void addContactInfo(ContactInfo contactInfo) {
        contactInfos.add(contactInfo);
        contactInfo.setClinic(this);
    }

    public void removeContactInfo(ContactInfo contactInfo) {
        contactInfos.remove(contactInfo);
        contactInfo.setClinic(null);
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setClinic(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setClinic(null);
    }

    public void addBusinessHours(BusinessHours hours) {
        businessHours.add(hours);
        hours.setClinic(this);
    }

    public void removeBusinessHours(BusinessHours hours) {
        businessHours.remove(hours);
        hours.setClinic(null);
    }

    public void addHoliday(Holiday holiday) {
        holidays.add(holiday);
        holiday.setClinic(this);
    }

    public void removeHoliday(Holiday holiday) {
        holidays.remove(holiday);
        holiday.setClinic(null);
    }

    public enum ClinicStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}