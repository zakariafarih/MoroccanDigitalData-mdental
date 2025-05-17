package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByClinicId(UUID clinicId);

    List<Address> findByClinicIdAndType(UUID clinicId, Address.AddressType type);

    Optional<Address> findByClinicIdAndPrimaryTrue(UUID clinicId);

    @Query("SELECT a FROM Address a WHERE a.clinic.id = :clinicId AND a.type = :type AND a.primary = true")
    Optional<Address> findPrimaryAddressByType(UUID clinicId, Address.AddressType type);
}