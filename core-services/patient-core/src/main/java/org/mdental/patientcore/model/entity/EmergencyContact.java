package org.mdental.patientcore.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.mdental.commons.model.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "emergency_contacts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "clinic_id = (SELECT p.clinic_id FROM patients p WHERE p.id = patient_id)")
public class EmergencyContact extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}