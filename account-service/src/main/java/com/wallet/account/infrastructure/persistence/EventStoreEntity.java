package com.wallet.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * JPA representation of one row in the event store.
 *
 * Why JSONB for {@code eventData}:
 * - Schema-flexible: new event variants need no DDL.
 * - Postgres can index into JSONB fields when we later need projections on event attributes.
 *
 * Why {@code version} is part of the unique key with {@code aggregateId}:
 * - It's the optimistic-locking primitive: two concurrent appenders cannot both win.
 * - It also doubles as the stream position used when replaying.
 */
@Entity
@Table(name = "event_store")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class EventStoreEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;
}
