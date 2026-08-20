package com.wallet.shared.event;

import java.time.Instant;

/**
 * Metadata embedded in every domain event so consumers can:
 * - deduplicate (eventId, idempotency at consumer side)
 * - propagate tracing (correlationId mirrors X-Correlation-Id header)
 * - evolve safely (version lets us reject old/new schema mismatches)
 */
public record EventMetadata(String eventId, Instant occurredAt, String correlationId, int version) {

    public static EventMetadata create(String correlationId, int version) {
        return new EventMetadata(
            com.wallet.shared.util.IdGenerator.newId(),
            Instant.now(),
            correlationId,
            version
        );
    }
}
