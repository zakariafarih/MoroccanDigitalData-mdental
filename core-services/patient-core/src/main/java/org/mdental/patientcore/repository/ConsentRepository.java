package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.Consent;
import org.mdental.patientcore.model.entity.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByPatientId(UUID patientId);

    Optional<Consent> findByPatientIdAndId(UUID patientId, UUID id);

    Optional<Consent> findByPatientIdAndType(UUID patientId, ConsentType type);
}