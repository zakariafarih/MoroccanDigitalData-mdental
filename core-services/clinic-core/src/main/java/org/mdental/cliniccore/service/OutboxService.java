package org.mdental.cliniccore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.entity.Outbox;
import org.mdental.cliniccore.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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

        } catch (Exception e) {
            log.error("Error saving event to outbox", e);
            // We don't want to break the main transaction for outbox failures
            // so we log the error but don't propagate it
        }
    }
}