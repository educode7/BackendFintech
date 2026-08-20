package com.wallet.shared.context;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Immutable correlation-id context propagated by Java 25 {@link ScopedValue}.
 *
 * <p>Why {@code ScopedValue} (Java 25) instead of {@code ThreadLocal} / SLF4J MDC:
 * <ul>
 *   <li><b>Automatically inheritable</b> across virtual threads and child
 *       {@code ScopedValue.where(...).run(...)} scopes — MDC loses the value
 *       the moment the thread switches.</li>
 *   <li><b>Immutable</b> for the lifetime of the scope — no risk of a
 *       downstream caller mutating it under us.</li>
 *   <li><b>Lifetime is bound to the scope</b> — no {@code finally} cleanup,
 *       no leaked values from a previous request.</li>
 * </ul>
 *
 * <p>Reads: {@link #currentOrNull()} or {@link #current()} from any code that
 * runs inside the filter's scoped run. Writes happen exclusively in
 * {@code CorrelationIdFilter} (servlet path) and the Kafka consumers
 * (messaging path).
 */
public final class CorrelationContext {

    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private CorrelationContext() { }

    /**
     * Returns the current correlation id or generates a new one if none is bound.
     * Safe to call from any thread/scope.
     */
    public static String currentOrNew() {
        String current = currentOrNull();
        return current != null ? current : UUID.randomUUID().toString();
    }

    /**
     * Returns the current correlation id, throwing if none is bound.
     * Use in code that is only reachable through a filter that already
     * established the scope.
     */
    public static String current() {
        try {
            return CORRELATION_ID.get();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("CorrelationContext not bound", e);
        }
    }

    /**
     * Returns the current correlation id, or {@code null} if none is bound.
     * <p>{@link ScopedValue#orElse} rejects {@code null} defaults, so we
     * catch {@link NoSuchElementException} which {@link ScopedValue#get}
     * throws when the value is unbound.
     */
    public static String currentOrNull() {
        try {
            return CORRELATION_ID.get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
