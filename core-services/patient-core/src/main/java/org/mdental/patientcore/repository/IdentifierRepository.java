package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.PatientIdentifier;
import org.mdental.patientcore.model.entity.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentifierRepository extends JpaRepository<PatientIdentifier, UUID> {

    List<PatientIdentifier> findByPatientId(UUID patientId);

    Optional<PatientIdentifier> findByPatientIdAndId(UUID patientId, UUID id);

    List<PatientIdentifier> findByPatientIdAndType(UUID patientId, IdentifierType type);

    Optional<PatientIdentifier> findByTypeAndValue(IdentifierType type, String value);
}