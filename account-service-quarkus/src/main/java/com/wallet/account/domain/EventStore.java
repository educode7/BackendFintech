package com.wallet.account.domain;

import java.util.List;
import java.util.Optional;

import com.wallet.shared.event.AccountEvent;

/**
 * Port: Event Store for Account aggregate.
 * Append-only log of domain events. State is derived by replaying events.
 */
public interface EventStore {

    /**
     * Append events to the stream for the given aggregate.
     * Uses optimistic concurrency: expectedVersion must match current stream version.
     *
     * @param aggregateId   account ID
     * @param events        events to append
     * @param expectedVersion current version before append (for concurrency control)
     * @throws com.wallet.account.domain.exception.ConcurrentModificationException
     *         if expectedVersion doesn't match
     */
    void appendEvents(String aggregateId, List<AccountEvent> events, long expectedVersion);

    /**
     * Load all events for an account.
     */
    List<AccountEvent> loadEvents(String aggregateId);

    /**
     * Load events after a specific version (for incremental projection).
     */
    List<AccountEvent> loadEventsAfter(String aggregateId, long afterVersion);
}
