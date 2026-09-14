package com.wallet.payment.domain.exception;

/**
 * Payment not found by the given ID.
 */
public final class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super("payment not found: " + paymentId);
        this.paymentId = paymentId;
    }

    public String paymentId() {
        return paymentId;
    }
}
