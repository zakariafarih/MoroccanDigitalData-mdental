package org.mdental.cliniccore.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "business_hours",
        indexes = {
                @Index(name = "idx_hours_clinic_id", columnList = "clinic_id"),
                @Index(name = "idx_hours_day_active", columnList = "day_of_week, active"),
                @Index(name = "idx_hours_valid_dates", columnList = "valid_from, valid_to")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class BusinessHours extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    // Sequence number to support multiple slots per day
    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "active", nullable = false)
    private Boolean active;

    // Seasonal override support
    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "label")
    private String label; // e.g., "Morning", "Afternoon", "Summer Hours"
}