package com.wallet.payment.infrastructure.web.exception;

/**
 * Raised when the database rejects an insert because the idempotency_key
 * already exists (UNIQUE constraint). Mapped to HTTP 409 Conflict.
 */
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String idempotencyKey) {
        super("payment with idempotency key already exists: " + idempotencyKey);
    }
}
