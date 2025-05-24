package org.mdental.authcore.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Outbox;
import org.mdental.authcore.domain.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes outbox events to Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaOutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mdental.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${mdental.outbox.batch-size:100}")
    private int batchSize;

    @Value("${mdental.outbox.kafka-topic-prefix:mdental.}")
    private String topicPrefix;

    /**
     * Process outbox entries periodically.
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    @Transactional
    public void processOutbox() {
        // Create pageable for optimized batch processing
        PageRequest pageable = PageRequest.of(0, batchSize);
        Page<Outbox> page = outboxRepository.findAllByDeadLetterFalseAndRetryCountLessThanOrderByCreatedAtAsc(
                maxRetries, pageable);

        if (page.hasContent()) {
            log.info("Processing {} outbox entries", page.getContent().size());

            List<UUID> successIds = new ArrayList<>();
            List<UUID> failureIds = new ArrayList<>();

            for (Outbox entry : page) {
                try {
                    // Extract event from outbox entry
                    JsonNode payload = objectMapper.readTree(entry.getPayload());

                    // Determine topic name: mdental.{aggregateType}.{eventType}
                    String topic = topicPrefix + entry.getAggregateType().toLowerCase() + "." +
                            entry.getEventType().toLowerCase();

                    // Create CloudEvents 1.0 compliant message
                    CloudEvent cloudEvent = new CloudEvent(
                            UUID.randomUUID().toString(), // unique ID for this event
                            entry.getEventType(),
                            entry.getAggregateType(),
                            entry.getAggregateId().toString(),
                            Instant.now().toString(),
                            payload
                    );

                    // Send to Kafka
                    CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                            topic,
                            entry.getAggregateId().toString(), // use aggregate ID as key for partitioning
                            cloudEvent
                    );

                    // Process result
                    future.whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Successfully sent event to Kafka: topic={}, key={}",
                                    topic, entry.getAggregateId());
                        } else {
                            log.error("Failed to send event to Kafka: {}", ex.getMessage(), ex);
                        }
                    });

                    // Add to success list - we consider the event successfully processed when it's sent to Kafka
                    successIds.add(entry.getId());

                } catch (Exception e) {
                    log.error("Error processing outbox entry {}: {}", entry.getId(), e.getMessage(), e);
                    // Track failed processing
                    failureIds.add(entry.getId());
                }
            }

            // Batch delete successful entries
            if (!successIds.isEmpty()) {
                outboxRepository.deleteAllById(successIds);
                log.debug("Deleted {} successfully processed outbox entries", successIds.size());
            }

            // Increment retry count for failed entries
            if (!failureIds.isEmpty()) {
                outboxRepository.incrementRetryCount(failureIds);
                log.warn("Incremented retry count for {} failed outbox entries", failureIds.size());

                // Mark as dead letter if max retries reached
                List<UUID> deadLetterIds = page.stream()
                        .filter(o -> failureIds.contains(o.getId()) && o.getRetryCount() >= maxRetries - 1)
                        .map(Outbox::getId)
                        .toList();

                if (!deadLetterIds.isEmpty()) {
                    outboxRepository.markAsDeadLetter(deadLetterIds);
                    log.warn("Marked {} entries as dead letter", deadLetterIds.size());
                }
            }
        }
    }

    /**
     * CloudEvent 1.0 compliant event structure.
     */
    private record CloudEvent(
            String id,
            String type,
            String source,
            String subject,
            String time,
            JsonNode data
    ) {}
}