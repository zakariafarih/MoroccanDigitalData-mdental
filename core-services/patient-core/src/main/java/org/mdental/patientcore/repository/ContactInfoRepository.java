package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.ContactInfo;
import org.mdental.patientcore.model.entity.ContactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfo, UUID> {

    List<ContactInfo> findByPatientId(UUID patientId);

    Page<ContactInfo> findByPatientId(UUID patientId, Pageable pageable);

    Optional<ContactInfo> findByPatientIdAndId(UUID patientId, UUID id);

    List<ContactInfo> findByPatientIdAndType(UUID patientId, ContactType type);

    List<ContactInfo> findByPatientIdAndIsPrimaryTrue(UUID patientId);

    List<ContactInfo> findByPatientIdAndTypeAndIsPrimaryTrue(UUID patientId, ContactType type);

    @Modifying
    @Query("UPDATE ContactInfo c SET c.isPrimary = false WHERE c.patientId = :patientId AND c.type = :type AND c.isPrimary = true")
    void resetPrimaryFlags(@Param("patientId") UUID patientId, @Param("type") ContactType type);
}