package com.wallet.payment.domain;

import java.util.List;

/**
 * Port: transactional outbox persistence.
 * Events are stored here in the same transaction as business data.
 * A separate poller reads and publishes them to Kafka.
 */
public interface OutboxRepository {

    /**
     * Save an outbox event (within the same transaction as business data).
     */
    void save(OutboxEvent event);

    /**
     * Load unpublished events ordered by creation time.
     */
    List<OutboxEvent> findUnpublished(int limit);

    /**
     * Mark events as published after successful Kafka send.
     */
    void markPublished(List<String> eventIds);
}
