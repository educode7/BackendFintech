package com.wallet.payment.infrastructure.persistence;

import com.wallet.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, String> {

    /**
     * Used by the idempotency layer to detect "key already used".
     * Note: the {@code UNIQUE} index on idempotency_key also enforces this at DB level.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
