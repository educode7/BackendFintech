package com.wallet.payment.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.payment.domain.Payment;

/**
 * Payment response DTO — the API surface.
 */
public record PaymentResponse(
        String id,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Map domain entity to response DTO.
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.userId(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
