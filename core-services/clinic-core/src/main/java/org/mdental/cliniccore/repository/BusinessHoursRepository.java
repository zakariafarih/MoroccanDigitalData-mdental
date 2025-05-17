package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    List<BusinessHours> findByClinicId(UUID clinicId);

    Optional<BusinessHours> findByClinicIdAndDayOfWeekAndSequence(
            UUID clinicId, DayOfWeek dayOfWeek, Integer sequence);

    List<BusinessHours> findByClinicIdAndDayOfWeek(UUID clinicId, DayOfWeek dayOfWeek);

    @Query("SELECT bh FROM BusinessHours bh WHERE bh.clinic.id = :clinicId AND bh.active = true")
    List<BusinessHours> findActiveHoursByClinicId(UUID clinicId);

    @Query("SELECT MAX(bh.sequence) FROM BusinessHours bh WHERE bh.clinic.id = :clinicId AND bh.dayOfWeek = :dayOfWeek")
    Integer findMaxSequenceForDay(UUID clinicId, DayOfWeek dayOfWeek);

    @Query("SELECT COUNT(bh) > 0 FROM BusinessHours bh " +
            "WHERE bh.clinic.id = :clinicId AND bh.dayOfWeek = :dayOfWeek " +
            "AND bh.id != :exceptId " +
            "AND NOT (:closeTime <= bh.openTime OR :openTime >= bh.closeTime)")
    boolean existsOverlappingBusinessHours(
            UUID clinicId,
            DayOfWeek dayOfWeek,
            LocalTime openTime,
            LocalTime closeTime,
            UUID exceptId);

    @Query("SELECT COUNT(bh) > 0 FROM BusinessHours bh " +
            "WHERE bh.clinic.id = :clinicId AND bh.dayOfWeek = :dayOfWeek " +
            "AND NOT (:closeTime <= bh.openTime OR :openTime >= bh.closeTime)")
    boolean existsOverlappingBusinessHours(
            UUID clinicId,
            DayOfWeek dayOfWeek,
            LocalTime openTime,
            LocalTime closeTime);

    @Query("SELECT bh FROM BusinessHours bh " +
            "WHERE bh.clinic.id = :clinicId " +
            "AND bh.dayOfWeek = :dayOfWeek " +
            "AND (bh.validFrom IS NULL OR bh.validFrom <= :date) " +
            "AND (bh.validTo IS NULL OR bh.validTo >= :date) " +
            "AND bh.active = true " +
            "ORDER BY bh.openTime")
    List<BusinessHours> findActiveHoursByDayAndDate(UUID clinicId, DayOfWeek dayOfWeek, LocalDate date);
}