package com.wallet.payment.domain;

import com.wallet.shared.money.Money;

/**
 * Port: event publishing to Kafka.
 * No framework dependency — implementation lives in infrastructure/kafka.
 */
public interface EventPublisher {

    /**
     * Publish a PaymentCompletedEvent after successful processing.
     *
     * @param paymentId   payment ID
     * @param accountId   target account for deposit
     * @param userId      payment owner
     * @param amount      payment amount
     * @param status      final status ("COMPLETED" or "FAILED")
     * @param correlationId correlation ID for distributed tracing
     */
    void publishPaymentCompleted(String paymentId, String accountId, String userId, Money amount,
                                 String status, String correlationId);
}
