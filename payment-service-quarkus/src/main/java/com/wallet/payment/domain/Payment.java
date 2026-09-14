package com.wallet.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;

import com.wallet.shared.money.Money;

/**
 * Payment aggregate root.
 * Pure domain object — no framework dependencies (no JPA, no Quarkus, no Redis).
 * <p>
 * State transitions: PENDING → PROCESSING → COMPLETED | FAILED
 * Terminal states: COMPLETED, FAILED — no further transitions allowed.
 */
public final class Payment {

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED;

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED;
        }
    }

    private final String id;
    private final String userId;
    private final Money amount;
    private final String idempotencyKey;
    private final Status status;
    private final long version;
    private final java.time.Instant createdAt;
    private final java.time.Instant updatedAt;

    private Payment(String id, String userId, Money amount, String idempotencyKey,
                    Status status, long version, java.time.Instant createdAt, java.time.Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Factory: create a new payment in PENDING state.
     */
    public static Payment create(String id, String userId, Money amount, String idempotencyKey) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        java.time.Instant now = java.time.Instant.now();
        return new Payment(id, userId, amount, idempotencyKey, Status.PENDING, 0, now, now);
    }

    /**
     * Factory: reconstitute from persistence (event store or JPA).
     */
    public static Payment of(String id, String userId, Money amount, String idempotencyKey,
                             Status status, long version, java.time.Instant createdAt, java.time.Instant updatedAt) {
        return new Payment(id, userId, amount, idempotencyKey, status, version, createdAt, updatedAt);
    }

    /**
     * Transition to PROCESSING.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment startProcessing() {
        assertNotTerminal();
        assertStatus(Status.PENDING, "PROCESSING");
        return new Payment(id, userId, amount, idempotencyKey, Status.PROCESSING,
                version + 1, createdAt, java.time.Instant.now());
    }

    /**
     * Transition to COMPLETED.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment complete() {
        assertNotTerminal();
        assertStatus(Status.PROCESSING, "COMPLETED");
        return new Payment(id, userId, amount, idempotencyKey, Status.COMPLETED,
                version + 1, createdAt, java.time.Instant.now());
    }

    /**
     * Transition to FAILED.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment fail() {
        assertNotTerminal();
        assertStatus(Status.PROCESSING, "FAILED");
        return new Payment(id, userId, amount, idempotencyKey, Status.FAILED,
                version + 1, createdAt, java.time.Instant.now());
    }

    private void assertNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot transition from terminal state " + status);
        }
    }

    private void assertStatus(Status expected, String target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + target);
        }
    }

    // --- Getters ---

    public String id() { return id; }
    public String userId() { return userId; }
    public Money amount() { return amount; }
    public String idempotencyKey() { return idempotencyKey; }
    public Status status() { return status; }
    public long version() { return version; }
    public java.time.Instant createdAt() { return createdAt; }
    public java.time.Instant updatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment p)) return false;
        return id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Payment[id=%s, userId=%s, amount=%s, status=%s, version=%d]"
                .formatted(id, userId, amount, status, version);
    }
}
