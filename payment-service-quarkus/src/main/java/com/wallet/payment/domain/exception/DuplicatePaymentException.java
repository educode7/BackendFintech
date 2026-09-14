package com.wallet.payment.domain.exception;

/**
 * Duplicate payment detected — same idempotency key already processed.
 */
public final class DuplicatePaymentException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicatePaymentException(String idempotencyKey) {
        super("payment with idempotency key already exists: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
