package com.wallet.payment.application;

import com.wallet.shared.money.Money;

/**
 * Inbound command to create + process a payment.
 * Carrier-only — no behavior, validated at the controller boundary.
 */
public record ProcessPaymentCommand(String userId, Money amount, String idempotencyKey) { }
