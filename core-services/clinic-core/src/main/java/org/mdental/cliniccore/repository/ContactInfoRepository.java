package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfo, UUID> {

    List<ContactInfo> findByClinicId(UUID clinicId);

    List<ContactInfo> findByClinicIdAndType(UUID clinicId, ContactInfo.ContactType type);

    Optional<ContactInfo> findByClinicIdAndPrimaryTrue(UUID clinicId);

    @Query("SELECT c FROM ContactInfo c WHERE c.clinic.id = :clinicId AND c.type = :type AND c.primary = true")
    Optional<ContactInfo> findPrimaryContactByType(UUID clinicId, ContactInfo.ContactType type);
}