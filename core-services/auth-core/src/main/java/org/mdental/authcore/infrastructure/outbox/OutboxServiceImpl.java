package org.mdental.authcore.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Outbox;
import org.mdental.authcore.domain.repository.OutboxRepository;
import org.mdental.authcore.domain.service.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the outbox pattern for reliable event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * Save an event to the outbox.
     * Uses a new transaction to ensure the event is saved even if the parent transaction fails.
     *
     * @param aggregateType the type of the aggregate (e.g. "User", "Tenant")
     * @param aggregateId the ID of the aggregate
     * @param eventType the type of the event
     * @param oldValue the old value (for update events)
     * @param newValue the new value
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEvent(String aggregateType, UUID aggregateId, String eventType, Object oldValue, Object newValue) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();

            if (oldValue != null) {
                JsonNode oldValueNode = objectMapper.valueToTree(oldValue);
                payload.set("oldValue", oldValueNode);
            }

            if (newValue != null) {
                JsonNode newValueNode = objectMapper.valueToTree(newValue);
                payload.set("newValue", newValueNode);
            }

            Outbox outboxEntry = Outbox.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxEntry);

            // Track metrics
            meterRegistry.counter("outbox.events.created",
                            "aggregateType", aggregateType,
                            "eventType", eventType)
                    .increment();

            log.debug("Saved outbox event: {} for {}", eventType, aggregateType);

        } catch (Exception e) {
            log.error("Error saving event to outbox", e);
            // Track error
            meterRegistry.counter("outbox.events.errors").increment();
        }
    }
}