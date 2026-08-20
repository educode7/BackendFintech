package com.wallet.payment.infrastructure.web.dto;

import com.wallet.shared.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inbound request body for POST /api/v1/payments.
 * Idempotency-Key is required as a header (see controller), not in the body,
 * because it is a transport-layer concern (retry semantics) rather than a
 * business attribute of the payment itself.
 */
public record ProcessPaymentRequest(
    @NotBlank @Size(max = 64) String userId,
    @NotNull Money amount
) { }
