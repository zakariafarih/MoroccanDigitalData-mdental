package org.mdental.authcore.infrastructure.jpa;

import java.util.List;
import java.util.UUID;
import org.mdental.authcore.domain.model.Outbox;
import org.mdental.authcore.domain.repository.OutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepositoryImpl extends OutboxRepository, JpaRepository<Outbox, UUID> {
    @Override
    Page<Outbox> findAllByDeadLetterFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetries, Pageable pageable);

    @Override
    @Modifying
    @Query("UPDATE Outbox o SET o.retryCount = o.retryCount + 1 WHERE o.id IN :ids")
    void incrementRetryCount(@Param("ids") List<UUID> ids);

    @Override
    @Modifying
    @Query("UPDATE Outbox o SET o.deadLetter = true WHERE o.id IN :ids")
    void markAsDeadLetter(@Param("ids") List<UUID> ids);
}