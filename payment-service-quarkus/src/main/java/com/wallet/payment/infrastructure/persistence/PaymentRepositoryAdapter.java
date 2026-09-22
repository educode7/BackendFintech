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
 * Infrastructure adapter: PaymentRepository implementation using classic JPA.
 * <p>
 * Uses EntityManager with @Transactional for persistence.
 */
@ApplicationScoped
public class PaymentRepositoryAdapter implements PaymentRepository {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Persists a new payment.
     * <p>
     * Uses persist() with Hibernate-generated UUID (fromDomainNew does not set ID).
     * After persist, copies the generated ID back into the domain object.
     */
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.fromDomainNew(payment);
        entityManager.persist(entity);
        entityManager.flush();
        // Copy the Hibernate-generated ID back to the domain, preserving ALL extended fields
        return Payment.of(
                entity.getId().toString(),
                payment.accountId(),
                payment.userId(),
                payment.amount(),
                payment.idempotencyKey(),
                payment.status(),
                entity.getVersion(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.paymentType(),
                payment.beneficiaryName(), payment.beneficiaryDocumentType(), payment.beneficiaryDocumentNumber(),
                payment.beneficiaryAccountNumber(), payment.beneficiaryBankCode(), payment.beneficiaryBankName(),
                payment.senderName(), payment.senderDocumentType(), payment.senderDocumentNumber(),
                payment.reference(), payment.externalReference(),
                payment.channel(), payment.ipAddress(), payment.userAgent(),
                payment.processedAt(), payment.failedAt(), payment.failureReason(), payment.retryCount(),
                payment.feeAmount()
        );
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

    @Override
    public List<Payment> findByUserIdPaginated(String userId, int offset, int limit) {
        return entityManager
                .createQuery("SELECT p FROM PaymentEntity p WHERE p.userId = :userId ORDER BY p.createdAt DESC",
                        PaymentEntity.class)
                .setParameter("userId", userId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return entityManager
                .createQuery("SELECT COUNT(p) FROM PaymentEntity p WHERE p.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
