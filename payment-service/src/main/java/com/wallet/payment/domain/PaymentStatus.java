package com.wallet.payment.domain;

/**
 * Payment lifecycle. Transitions:
 *   PENDING -> PROCESSING -> COMPLETED
 *                        \-> FAILED
 *
 * Why these states and not "DONE" everywhere:
 * PENDING represents a payment the API accepted but has not yet touched Kafka with;
 * PROCESSING means we are mid-flight (transaction committed, event not yet acked).
 * COMPLETED is terminal-success, FAILED is terminal-failure.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
