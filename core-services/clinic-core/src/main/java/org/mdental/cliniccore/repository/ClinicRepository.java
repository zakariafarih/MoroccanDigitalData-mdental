package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.Clinic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends TenantAwareRepository<Clinic, UUID> {

    @Query("SELECT c FROM Clinic c WHERE c.realm = :realm AND c.deletedAt IS NULL")
    Optional<Clinic> findByRealm(@Param("realm") String realm);

    Optional<Clinic> findBySlug(String slug);

    Optional<Clinic> findByName(String name);

    @Query("SELECT c FROM Clinic c WHERE c.status = :status")
    List<Clinic> findAllByStatus(Clinic.ClinicStatus status);

    Page<Clinic> findByRealmAndNameContainingIgnoreCase(
            String realm, String name, Pageable pageable);

    Page<Clinic> findByNameContainingIgnoreCase(
            String name, Pageable pageable);

    @EntityGraph(attributePaths = {"contactInfos", "addresses", "businessHours", "holidays"})
    @Query("SELECT c FROM Clinic c WHERE c.id = :id")
    Optional<Clinic> findByIdWithAllRelationships(@Param("id") UUID id);

    @Override
    default List<Clinic> findAllByClinicRealm(String realm) {
        return findByRealm(realm).stream().toList();
    }
}