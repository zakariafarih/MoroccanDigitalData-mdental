package org.mdental.cliniccore.repository;

import org.mdental.cliniccore.model.entity.Outbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    Page<Outbox> findAllByOrderByCreatedAtAsc(Pageable pageable);
}