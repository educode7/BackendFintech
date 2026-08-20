package com.wallet.payment.infrastructure.web.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String id) {
        super("payment not found: " + id);
    }
}
