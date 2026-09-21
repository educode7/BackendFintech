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
        String idempotencyKey,
        // Payment type
        String paymentType,
        // Beneficiary info
        String beneficiaryName,
        String beneficiaryDocumentType,
        String beneficiaryDocumentNumber,
        String beneficiaryAccountNumber,
        String beneficiaryBankCode,
        String beneficiaryBankName,
        // Sender info
        String senderName,
        String senderDocumentType,
        String senderDocumentNumber,
        // Reference
        String reference,
        String externalReference,
        // Routing
        String channel,
        String ipAddress,
        String userAgent,
        // Fees
        Money feeAmount
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

    /**
     * Backward-compatible constructor for basic payments.
     */
    public ProcessPaymentCommand(String accountId, String userId, Money amount, String idempotencyKey) {
        this(accountId, userId, amount, idempotencyKey,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null,
                null, null, null,
                null);
    }
}
