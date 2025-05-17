package org.mdental.cliniccore.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "contact_info", indexes = {
        @Index(name = "idx_contact_clinic_id", columnList = "clinic_id"),
        @Index(name = "idx_contact_type", columnList = "type"),
        @Index(name = "idx_contact_primary", columnList = "is_primary")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ContactInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ContactType type;

    @Column(name = "label")
    private String label;

    @Column(name = "\"value\"", nullable = false) // Quoting the reserved keyword
    private String value;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary;

    public enum ContactType {
        PHONE, MOBILE, FAX, EMAIL, WEBSITE
    }
}