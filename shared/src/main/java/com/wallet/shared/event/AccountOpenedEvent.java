package com.wallet.shared.event;

import com.wallet.shared.money.Money;

/**
 * Emitted by account-service when an account is created.
 * initialBalance is informational; the canonical truth lives in the event store.
 */
public record AccountOpenedEvent(
    String accountId,
    String userId,
    Money initialBalance,
    EventMetadata metadata
) implements AccountEvent {

    @Override
    public String accountId() { return accountId; }
}
