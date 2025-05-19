package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.PatientPortalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientPortalAccountRepository extends JpaRepository<PatientPortalAccount, UUID> {

    Optional<PatientPortalAccount> findByPatientId(UUID patientId);

    Optional<PatientPortalAccount> findByUsername(String username);
}