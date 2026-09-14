package com.wallet.payment.domain.exception;

/**
 * Idempotency-Key header is missing from a state-changing request.
 */
public final class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required for this operation");
    }
}
