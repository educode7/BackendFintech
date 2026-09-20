package com.wallet.payment.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the transactional outbox.
 * Events are written in the same transaction as business data,
 * then a polling publisher sends them to Kafka.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public OutboxEventEntity() {}

    public OutboxEventEntity(String eventType, String aggregateId, String aggregateType,
                             String payload, String correlationId) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.published = false;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
