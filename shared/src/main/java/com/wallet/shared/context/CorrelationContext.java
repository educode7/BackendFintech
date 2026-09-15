package com.wallet.shared.context;

import java.lang.ScopedValue;
import java.util.Optional;

/**
 * Request-scoped correlation identifier propagated across asynchronous boundaries.
 *
 * <p>Uses {@link ScopedValue} (final in Java 25) so the ID travels automatically
 * through virtual-thread forks without explicit parameter threading.</p>
 */
public final class CorrelationContext {

    private CorrelationContext() {
    }

    private static final ScopedValue<String> HOLDER = ScopedValue.newInstance();

    /**
     * Runs {@code action} with {@code correlationId} bound to the current
     * virtual-thread scope.  The value is visible in the calling thread and
     * any virtual threads forked from it.
     *
     * @param correlationId non-null correlation identifier
     * @param action        operation to execute within the bound scope
     */
    public static void run(String correlationId, Runnable action) {
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId must not be null");
        }
        ScopedValue.where(HOLDER, correlationId).run(action);
    }

    /**
     * Returns the current correlation identifier.
     *
     * @return an {@link Optional} containing the value if one is bound,
     *         or empty if called outside a bound scope
     */
    public static Optional<String> get() {
        if (HOLDER.isBound()) {
            return Optional.of(HOLDER.get());
        }
        return Optional.empty();
    }
}
