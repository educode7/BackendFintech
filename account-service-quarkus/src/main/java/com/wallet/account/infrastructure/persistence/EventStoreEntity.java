package com.wallet.account.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity for the event store.
 * Append-only — events are never updated or deleted.
 * The published flag enables the transactional outbox pattern.
 */
@Entity
@Table(name = "event_store", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_store_aggregate_version", columnNames = {"aggregate_id", "version"})
})
public class EventStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private long version;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EventStoreEntity() {}

    public EventStoreEntity(String aggregateId, String eventType, String payload,
                            long version, String correlationId) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.version = version;
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
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public long getVersion() { return version; }
    public String getCorrelationId() { return correlationId; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
