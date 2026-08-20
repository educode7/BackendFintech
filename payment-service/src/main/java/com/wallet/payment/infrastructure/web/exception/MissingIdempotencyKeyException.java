package com.wallet.payment.infrastructure.web.exception;

/**
 * Thrown when a mutation request arrives without an Idempotency-Key header.
 * Idempotency keys are non-optional for write endpoints on this service.
 */
public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required for this operation");
    }
}
