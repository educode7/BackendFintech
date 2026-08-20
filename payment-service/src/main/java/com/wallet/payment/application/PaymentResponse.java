package com.wallet.payment.application;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound DTO. Separate from the JPA entity so we can:
 * - Evolve the wire format without touching persistence.
 * - Keep JSON shape stable across endpoints.
 */
public record PaymentResponse(
    String id,
    String userId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(),
            p.getUserId(),
            p.getAmount(),
            p.getCurrency(),
            p.getStatus(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
