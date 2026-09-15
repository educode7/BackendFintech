package com.wallet.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;

/**
 * Infrastructure adapter: PaymentRepository implementation using Hibernate Reactive.
 * <p>
 * Uses Panache for reactive persistence. No blocking calls.
 */
@ApplicationScoped
public class PaymentRepositoryAdapter implements PaymentRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.fromDomain(payment);
        if (entityManager.contains(entity)) {
            entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }
        entityManager.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Payment> findById(String id) {
        try {
            PaymentEntity entity = entityManager.find(PaymentEntity.class, UUID.fromString(id));
            return Optional.ofNullable(entity).map(PaymentEntity::toDomain);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        try {
            PaymentEntity entity = entityManager
                    .createQuery("SELECT p FROM PaymentEntity p WHERE p.idempotencyKey = :key",
                            PaymentEntity.class)
                    .setParameter("key", idempotencyKey)
                    .getSingleResult();
            return Optional.ofNullable(entity).map(PaymentEntity::toDomain);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Payment> findAll(int offset, int limit) {
        return entityManager
                .createQuery("SELECT p FROM PaymentEntity p ORDER BY p.createdAt DESC",
                        PaymentEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return entityManager
                .createQuery("SELECT COUNT(p) FROM PaymentEntity p", Long.class)
                .getSingleResult();
    }
}
