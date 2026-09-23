package com.wallet.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationRepository;

/**
 * Infrastructure adapter: NotificationRepository using Hibernate Reactive.
 */
@ApplicationScoped
public class NotificationRepositoryAdapter implements NotificationRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationEntity.fromDomain(notification);
        NotificationEntity existing = entityManager.find(NotificationEntity.class, entity.getId());
        if (existing != null) {
            existing.setStatus(entity.getStatus());
            existing.setSubject(entity.getSubject());
            existing.setBody(entity.getBody());
            existing.setSentAt(entity.getSentAt());
            existing.setReadAt(entity.getReadAt());
            entityManager.flush();
            return existing.toDomain();
        } else {
            entityManager.persist(entity);
            entityManager.flush();
            return entity.toDomain();
        }
    }

    @Override
    public Optional<Notification> findById(String id) {
        try {
            NotificationEntity entity = entityManager.find(NotificationEntity.class, id);
            return Optional.ofNullable(entity).map(NotificationEntity::toDomain);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Notification> findByUserId(String userId, int offset, int limit) {
        return entityManager
                .createQuery("SELECT n FROM NotificationEntity n WHERE n.userId = :userId ORDER BY n.createdAt DESC",
                        NotificationEntity.class)
                .setParameter("userId", userId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return entityManager
                .createQuery("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    @Transactional
    public Optional<Notification> markAsRead(String id) {
        NotificationEntity entity = entityManager.find(NotificationEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }
        if (entity.getReadAt() != null) {
            // Already read: idempotent, return current state unchanged.
            return Optional.of(entity.toDomain());
        }
        entity.setReadAt(Instant.now());
        entityManager.flush();
        return Optional.of(entity.toDomain());
    }

    @Override
    @Transactional
    public long markAllAsRead(String userId) {
        // Bulk update only rows still unread; bypasses the persistence context on purpose.
        return entityManager
                .createQuery("UPDATE NotificationEntity n SET n.readAt = :now "
                        + "WHERE n.userId = :userId AND n.readAt IS NULL")
                .setParameter("now", Instant.now())
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
