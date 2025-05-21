package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findByPatientId(UUID patientId);

    Optional<EmergencyContact> findByPatientIdAndId(UUID patientId, UUID id);

    List<EmergencyContact> findByPatientIdAndIsPrimaryTrue(UUID patientId);

    @Modifying
    @Query("UPDATE EmergencyContact e SET e.isPrimary = false WHERE e.patientId = :patientId AND e.isPrimary = true")
    void resetPrimaryFlags(@Param("patientId") UUID patientId);
}