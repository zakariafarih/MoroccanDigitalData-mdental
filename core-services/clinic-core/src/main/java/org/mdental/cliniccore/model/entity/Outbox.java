package org.mdental.cliniccore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * How many times we’ve retried delivering this event.
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * If true, this event has been moved to the dead-letter queue.
     */
    @Column(name = "dead_letter", nullable = false)
    private boolean deadLetter = false;

    @Version
    @Column(name = "version")
    private int version;
}
