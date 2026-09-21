package com.wallet.payment.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.payment.domain.Payment;

/**
 * Payment response DTO — the API surface.
 */
public record PaymentResponse(
        String id,
        String accountId,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt,
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
        // Processing
        Instant processedAt,
        Instant failedAt,
        String failureReason,
        int retryCount,
        // Fees
        BigDecimal feeAmount,
        String feeCurrency
) {
    /**
     * Map domain entity to response DTO.
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.accountId(),
                payment.userId(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt(),
                // Payment type
                payment.paymentType(),
                // Beneficiary info
                payment.beneficiaryName(),
                payment.beneficiaryDocumentType(),
                payment.beneficiaryDocumentNumber(),
                payment.beneficiaryAccountNumber(),
                payment.beneficiaryBankCode(),
                payment.beneficiaryBankName(),
                // Sender info
                payment.senderName(),
                payment.senderDocumentType(),
                payment.senderDocumentNumber(),
                // Reference
                payment.reference(),
                payment.externalReference(),
                // Routing
                payment.channel(),
                // Processing
                payment.processedAt(),
                payment.failedAt(),
                payment.failureReason(),
                payment.retryCount(),
                // Fees
                payment.feeAmount() != null ? payment.feeAmount().amount() : null,
                payment.feeAmount() != null ? payment.feeAmount().currency() : null
        );
    }
}
