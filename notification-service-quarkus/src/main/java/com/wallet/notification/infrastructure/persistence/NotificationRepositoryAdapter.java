package com.wallet.notification.infrastructure.persistence;

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
        if (entityManager.contains(entity)) {
            entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }
        entityManager.flush();
        return entity.toDomain();
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
}
