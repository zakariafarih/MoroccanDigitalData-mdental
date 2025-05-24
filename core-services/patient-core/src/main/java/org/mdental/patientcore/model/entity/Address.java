package org.mdental.patientcore.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.mdental.commons.model.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "addresses", indexes = {
        @Index(name = "idx_address_patient_id", columnList = "patient_id"),
        @Index(name = "idx_address_type", columnList = "type"),
        @Index(name = "idx_address_is_primary", columnList = "is_primary")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "clinic_id = (SELECT p.clinic_id FROM patients p WHERE p.id = patient_id)")
public class Address extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AddressType type;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "zip", nullable = false)
    private String zip;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}