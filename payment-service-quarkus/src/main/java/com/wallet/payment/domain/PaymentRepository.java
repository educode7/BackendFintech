package com.wallet.payment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.wallet.shared.money.Money;

/**
 * Port: payment persistence.
 * No framework dependency — implementation lives in infrastructure/persistence.
 */
public interface PaymentRepository {

    /**
     * Persist a new payment or update an existing one.
     */
    Payment save(Payment payment);

    /**
     * Find a payment by its internal ID.
     */
    Optional<Payment> findById(String id);

    /**
     * Find a payment by its idempotency key.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all payments with pagination.
     */
    List<Payment> findAll(int offset, int limit);

    /**
     * Count all payments.
     */
    long countAll();

    /**
     * Find payments by user ID with pagination.
     */
    List<Payment> findByUserIdPaginated(String userId, int offset, int limit);

    /**
     * Count payments by user ID.
     */
    long countByUserId(String userId);
}
