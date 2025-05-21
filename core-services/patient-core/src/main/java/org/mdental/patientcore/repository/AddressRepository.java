package org.mdental.patientcore.repository;

import org.mdental.patientcore.model.entity.Address;
import org.mdental.patientcore.model.entity.AddressType;
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
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByPatientId(UUID patientId);

    Page<Address> findByPatientId(UUID patientId, Pageable pageable);

    Optional<Address> findByPatientIdAndId(UUID patientId, UUID id);

    List<Address> findByPatientIdAndType(UUID patientId, AddressType type);

    List<Address> findByPatientIdAndIsPrimaryTrue(UUID patientId);

    List<Address> findByPatientIdAndTypeAndIsPrimaryTrue(UUID patientId, AddressType type);

    @Modifying
    @Query("UPDATE Address a SET a.isPrimary = false WHERE a.patientId = :patientId AND a.type = :type AND a.isPrimary = true")
    void resetPrimaryFlags(@Param("patientId") UUID patientId, @Param("type") AddressType type);
}