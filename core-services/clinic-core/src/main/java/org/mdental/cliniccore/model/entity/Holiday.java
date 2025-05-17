package org.mdental.cliniccore.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holiday_clinic_id", columnList = "clinic_id"),
        @Index(name = "idx_holiday_date", columnList = "date"),
        @Index(name = "idx_holiday_recurring", columnList = "recurring")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Holiday extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "recurring", nullable = false)
    private Boolean recurring;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private HolidayRuleType ruleType;

    @Column(name = "rule_pattern")
    private String rulePattern;

    @Builder.Default
    @Column(name = "is_half_day", nullable = false)
    private Boolean isHalfDay = false;

    @Column(name = "half_day_start")
    private LocalTime halfDayStart;

    @Column(name = "half_day_end")
    private LocalTime halfDayEnd;

    public enum HolidayRuleType {
        FIXED_DATE,     // Same date every year (e.g., Dec 25)
        PATTERN         // Rule-based (e.g., 3rd Monday of January)
    }
}