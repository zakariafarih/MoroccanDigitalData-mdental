package org.mdental.authcore.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Outbox;
import org.mdental.authcore.domain.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Background processor for outbox events.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    @Value("${mdental.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${mdental.outbox.batch-size:100}")
    private int batchSize;

    /**
     * Initialize metrics.
     */
    @PostConstruct
    void initMetrics() {
        meterRegistry.gauge("outbox.entries", this, p -> outboxRepository.count());
    }

    /**
     * Process outbox entries periodically.
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    @Transactional
    public void processOutbox() {
        // Create pageable for optimized batch processing
        PageRequest pageable = PageRequest.of(0, batchSize);
        Page<Outbox> page = outboxRepository.findAllByDeadLetterFalseAndRetryCountLessThanOrderByCreatedAtAsc(maxRetries, pageable);

        if (page.hasContent()) {
            log.info("Processing {} outbox entries", page.getContent().size());

            List<UUID> successIds = new ArrayList<>();
            List<UUID> failureIds = new ArrayList<>();

            for (Outbox entry : page) {
                try {
                    // In a real implementation, this would publish to Kafka or another message broker
                    log.info("Event: type={}, aggregateType={}, aggregateId={}, payload={}",
                            entry.getEventType(),
                            entry.getAggregateType(),
                            entry.getAggregateId(),
                            entry.getPayload());

                    successIds.add(entry.getId());

                    // Track successful processing
                    meterRegistry.counter("outbox.processing.success",
                            "eventType", entry.getEventType(),
                            "aggregateType", entry.getAggregateType()).increment();

                } catch (Exception e) {
                    log.error("Error processing outbox entry {}: {}", entry.getId(), e.getMessage());
                    // Track failed processing
                    failureIds.add(entry.getId());
                    // Increment error counter
                    meterRegistry.counter("outbox.processing.errors",
                            "eventType", entry.getEventType(),
                            "aggregateType", entry.getAggregateType()).increment();
                }
            }

            // Batch delete successful entries
            if (!successIds.isEmpty()) {
                outboxRepository.deleteAllByIdInBatch(successIds);
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
}