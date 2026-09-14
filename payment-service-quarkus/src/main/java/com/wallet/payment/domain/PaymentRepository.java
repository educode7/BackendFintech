package com.wallet.payment.domain;

import java.time.Instant;
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
}
