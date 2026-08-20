package com.wallet.shared.event;

import com.wallet.shared.money.Money;

/**
 * Emitted by payment-service when a payment reaches COMPLETED status.
 * account-service and notification-service consume it.
 */
public record PaymentCompletedEvent(
    String paymentId,
    String userId,
    Money amount,
    String status,
    EventMetadata metadata
) { }
