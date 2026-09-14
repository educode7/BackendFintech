package com.wallet.payment.domain;

import java.util.Optional;

/**
 * Port: idempotency store.
 * No framework dependency — implementation lives in infrastructure/cache.
 */
public interface IdempotencyStore {

    /**
     * State of an idempotency slot.
     */
    enum SlotState {
        /** Caller won the race — proceed with processing. */
        IN_PROGRESS,
        /** Operation completed — replay cached response. */
        COMPLETED,
        /** Operation failed — retry policy applies. */
        FAILED,
        /** Another caller is already processing this key — reject as duplicate. */
        DUPLICATE
    }

    /**
     * Result of an idempotency check.
     *
     * @param state    slot state
     * @param value    cached response payload (only if state is COMPLETED)
     */
    record Result(SlotState state, Optional<String> value) {}

    /**
     * Atomically claim an idempotency slot.
     * <p>
     * Returns {@link SlotState#IN_PROGRESS} if this caller won the race.
     * Returns the current state if another caller already claimed it.
     *
     * @param key      idempotency key
     * @param ttlHours time-to-live in hours
     * @return slot state after claim attempt
     */
    SlotState claim(String key, int ttlHours);

    /**
     * Mark a slot as COMPLETED with the serialized response.
     */
    void complete(String key, String responsePayload, int ttlHours);

    /**
     * Mark a slot as FAILED (allows retry per policy).
     */
    void markFailed(String key);

    /**
     * Retrieve a cached response for replay.
     */
    Optional<String> getCachedResponse(String key);
}
