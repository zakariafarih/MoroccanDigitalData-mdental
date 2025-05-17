package org.mdental.cliniccore.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.entity.Outbox;
import org.mdental.cliniccore.repository.OutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;

    @Scheduled(fixedRate = 5000) // 5 seconds
    @Transactional
    public void processOutbox() {
        // Create pageable for optimized batch processing
        PageRequest pageable = PageRequest.of(0, 100, Sort.by("createdAt"));
        Page<Outbox> page = outboxRepository.findAllByOrderByCreatedAtAsc(pageable);

        if (page.hasContent()) {
            log.info("Processing {} outbox entries", page.getContent().size());

            for (Outbox entry : page.getContent()) {
                try {
                    // In a real implementation, this would publish to a message broker
                    log.info("Event: type={}, aggregateType={}, aggregateId={}, payload={}",
                            entry.getEventType(),
                            entry.getAggregateType(),
                            entry.getAggregateId(),
                            entry.getPayload());

                    // Delete the entry after processing
                    outboxRepository.delete(entry);
                } catch (Exception e) {
                    log.error("Error processing outbox entry {}: {}", entry.getId(), e.getMessage());
                }
            }
        }
    }
}