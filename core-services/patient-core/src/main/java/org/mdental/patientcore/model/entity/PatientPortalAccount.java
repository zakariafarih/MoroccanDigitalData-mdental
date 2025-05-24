package org.mdental.patientcore.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.mdental.commons.model.BaseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "patient_portal_accounts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "clinic_id = (SELECT p.clinic_id FROM patients p WHERE p.id = patient_id)")
public class PatientPortalAccount extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "patient_portal_roles",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "last_login")
    private Instant lastLogin;

    @ElementCollection
    @CollectionTable(
            name = "patient_portal_preferences",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @MapKeyColumn(name = "preference_key")
    @Column(name = "preference_value")
    private Map<String, String> preferences = new HashMap<>();
}