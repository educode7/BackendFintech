package com.wallet.payment.domain;

import java.time.Instant;

/**
 * Domain object: outbox event for transactional outbox pattern.
 */
public record OutboxEvent(
    String id,
    String eventType,
    String aggregateId,
    String aggregateType,
    String payload,
    String correlationId,
    boolean published,
    Instant createdAt
) {
    public static OutboxEvent create(String eventType, String aggregateId, String aggregateType,
                                     String payload, String correlationId) {
        return new OutboxEvent(
            null,
            eventType,
            aggregateId,
            aggregateType,
            payload,
            correlationId,
            false,
            Instant.now()
        );
    }
}
