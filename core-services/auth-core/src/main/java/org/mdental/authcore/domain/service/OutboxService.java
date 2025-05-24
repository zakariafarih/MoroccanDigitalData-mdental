package org.mdental.authcore.domain.service;

import java.util.UUID;

/**
 * Service for managing the outbox pattern.
 */
public interface OutboxService {
    /**
     * Save an event to the outbox.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId the aggregate ID
     * @param eventType the event type
     * @param oldValue the old value (for update events)
     * @param newValue the new value
     */
    void saveEvent(String aggregateType, UUID aggregateId, String eventType, Object oldValue, Object newValue);
}