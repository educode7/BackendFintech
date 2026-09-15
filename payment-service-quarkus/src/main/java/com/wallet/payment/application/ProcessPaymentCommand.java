package com.wallet.payment.application;

import java.math.BigDecimal;

import com.wallet.shared.money.Money;

/**
 * Command to process a new payment.
 */
public record ProcessPaymentCommand(
        String accountId,
        String userId,
        Money amount,
        String idempotencyKey
) {
    public ProcessPaymentCommand {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
