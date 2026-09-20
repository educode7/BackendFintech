package com.wallet.account.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.account.domain.EventStore;
import com.wallet.account.domain.exception.ConcurrentModificationException;
import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;

/**
 * Infrastructure adapter: EventStore implementation using JPA.
 * <p>
 * Append-only — events are never updated or deleted.
 * Uses optimistic concurrency via UNIQUE(aggregate_id, version).
 * The published flag enables the transactional outbox pattern.
 */
@ApplicationScoped
public class JpaEventStore implements EventStore {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public void appendEvents(String aggregateId, List<AccountEvent> events, long expectedVersion) {
        long currentVersion = getCurrentVersion(aggregateId);

        if (currentVersion != expectedVersion) {
            throw new ConcurrentModificationException(aggregateId, expectedVersion, currentVersion);
        }

        long version = expectedVersion;
        for (AccountEvent event : events) {
            version++;
            String eventType = event.getClass().getSimpleName();
            String payload = com.wallet.shared.util.JsonUtil.toJson(event);

            EventStoreEntity entity = new EventStoreEntity(
                    aggregateId, eventType, payload, version,
                    event.metadata() != null ? event.metadata().correlationId() : null);

            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public List<AccountEvent> loadEvents(String aggregateId) {
        List<EventStoreEntity> entities = entityManager
                .createQuery("SELECT e FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.version ASC",
                        EventStoreEntity.class)
                .setParameter("aggregateId", aggregateId)
                .getResultList();

        return entities.stream()
                .map(this::toDomainEvent)
                .toList();
    }

    @Override
    public List<AccountEvent> loadEventsAfter(String aggregateId, long afterVersion) {
        List<EventStoreEntity> entities = entityManager
                .createQuery("SELECT e FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId AND e.version > :afterVersion ORDER BY e.version ASC",
                        EventStoreEntity.class)
                .setParameter("aggregateId", aggregateId)
                .setParameter("afterVersion", afterVersion)
                .getResultList();

        return entities.stream()
                .map(this::toDomainEvent)
                .toList();
    }

    /**
     * Find unpublished events for the outbox poller.
     */
    public List<EventStoreEntity> findUnpublished(int limit) {
        return entityManager
                .createQuery("SELECT e FROM EventStoreEntity e WHERE e.published = false ORDER BY e.createdAt ASC",
                        EventStoreEntity.class)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Mark events as published after successful Kafka send.
     */
    @Transactional
    public void markPublished(List<UUID> eventIds) {
        for (UUID id : eventIds) {
            EventStoreEntity entity = entityManager.find(EventStoreEntity.class, id);
            if (entity != null) {
                entity.markPublished();
            }
        }
        entityManager.flush();
    }

    private long getCurrentVersion(String aggregateId) {
        Long maxVersion = entityManager
                .createQuery("SELECT MAX(e.version) FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId",
                        Long.class)
                .setParameter("aggregateId", aggregateId)
                .getSingleResult();

        return maxVersion != null ? maxVersion : 0L;
    }

    private AccountEvent toDomainEvent(EventStoreEntity entity) {
        String payload = entity.getPayload();
        return switch (entity.getEventType()) {
            case "AccountOpenedEvent" ->
                com.wallet.shared.util.JsonUtil.fromJson(payload, AccountOpenedEvent.class);
            case "MoneyDepositedEvent" ->
                com.wallet.shared.util.JsonUtil.fromJson(payload, MoneyDepositedEvent.class);
            case "MoneyWithdrawnEvent" ->
                com.wallet.shared.util.JsonUtil.fromJson(payload, MoneyWithdrawnEvent.class);
            default -> throw new IllegalStateException("Unknown event type: " + entity.getEventType());
        };
    }
}
