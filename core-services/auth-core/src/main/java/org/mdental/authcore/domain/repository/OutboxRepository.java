package org.mdental.authcore.domain.repository;

import org.mdental.authcore.domain.model.Outbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for outbox operations.
 */
@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
    /**
     * Find unprocessed events that are not in dead-letter state.
     *
     * @param maxRetries the maximum retry count
     * @param pageable the pagination parameters
     * @return a page of outbox events
     */
    Page<Outbox> findAllByDeadLetterFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetries, Pageable pageable);

    /**
     * Increment the retry count for multiple events.
     *
     * @param ids the event IDs
     */
    @Modifying
    @Query("UPDATE Outbox o SET o.retryCount = o.retryCount + 1 WHERE o.id IN :ids")
    void incrementRetryCount(@Param("ids") List<UUID> ids);

    /**
     * Mark events as dead-letter (failed permanently).
     *
     * @param ids the event IDs
     */
    @Modifying
    @Query("UPDATE Outbox o SET o.deadLetter = true WHERE o.id IN :ids")
    void markAsDeadLetter(@Param("ids") List<UUID> ids);
}