package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    List<Holiday> findByClinicId(UUID clinicId);

    @Query("SELECT h FROM Holiday h WHERE h.clinic.id = :clinicId AND h.date >= :startDate AND h.date <= :endDate")
    List<Holiday> findByClinicIdAndDateBetween(UUID clinicId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT h FROM Holiday h WHERE h.clinic.id = :clinicId AND h.recurring = true")
    List<Holiday> findRecurringHolidaysByClinicId(UUID clinicId);
}