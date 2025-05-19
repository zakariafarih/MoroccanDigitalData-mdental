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
@Table(name = "patient_identifiers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "clinic_id = (SELECT p.clinic_id FROM patients p WHERE p.id = patient_id)")
public class PatientIdentifier extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IdentifierType type;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "issuer")
    private String issuer;
}