package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.Patient;
import org.mdental.patientcore.model.entity.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findByClinicId(UUID clinicId);

    @Query(value = "SELECT p FROM Patient p WHERE p.clinicId = :clinicId",
            countQuery = "SELECT COUNT(p) FROM Patient p WHERE p.clinicId = :clinicId")
    Page<Patient> findByClinicId(UUID clinicId, Pageable pageable);

    List<Patient> findByClinicIdAndStatus(UUID clinicId, PatientStatus status);

    Page<Patient> findByClinicIdAndStatus(UUID clinicId, PatientStatus status, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND " +
            "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.middleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Patient> findByClinicIdAndNameContainingIgnoreCase(
            @Param("clinicId") UUID clinicId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);
}