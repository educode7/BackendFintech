package com.wallet.payment.infrastructure.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * REST request DTO for ProcessPayment.
 */
public record ProcessPaymentRequest(
        @NotBlank @Size(max = 64) String accountId,
        @NotBlank @Size(max = 64) String userId,
        @NotNull Amount amount
) {
    public record Amount(
            @NotNull BigDecimal amount,
            @NotBlank String currency
    ) {}
}
