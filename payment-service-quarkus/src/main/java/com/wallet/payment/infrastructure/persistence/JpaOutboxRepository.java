package com.wallet.payment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.payment.domain.OutboxEvent;
import com.wallet.payment.domain.OutboxRepository;

/**
 * Infrastructure adapter: OutboxRepository implementation using JPA.
 */
@ApplicationScoped
public class JpaOutboxRepository implements OutboxRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public void save(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity(
                event.eventType(),
                event.aggregateId(),
                event.aggregateType(),
                event.payload(),
                event.correlationId()
        );
        entityManager.persist(entity);
        entityManager.flush();
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        List<OutboxEventEntity> entities = entityManager
                .createQuery(
                    "SELECT e FROM OutboxEventEntity e WHERE e.published = false ORDER BY e.createdAt ASC",
                    OutboxEventEntity.class)
                .setMaxResults(limit)
                .getResultList();

        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(List<String> eventIds) {
        for (String id : eventIds) {
            OutboxEventEntity entity = entityManager.find(OutboxEventEntity.class, UUID.fromString(id));
            if (entity != null) {
                entity.markPublished();
            }
        }
        entityManager.flush();
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId().toString(),
                entity.getEventType(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getPayload(),
                entity.getCorrelationId(),
                entity.isPublished(),
                entity.getCreatedAt()
        );
    }
}
