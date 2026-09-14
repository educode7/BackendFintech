package com.wallet.account.domain;

import com.wallet.shared.money.Money;

/**
 * Port: Domain event publishing to Kafka.
 */
public interface EventPublisher {

    /**
     * Publish an account domain event to Kafka.
     *
     * @param eventType     event type discriminator
     * @param aggregateId   account ID
     * @param payload       serialized event payload
     * @param correlationId distributed tracing correlation ID
     */
    void publish(String eventType, String aggregateId, String payload, String correlationId);
}
