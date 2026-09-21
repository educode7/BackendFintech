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
    EventMetadata metadata,
    AccountData accountData
) implements AccountEvent {

    @Override
    public String accountId() { return accountId; }

    /**
     * Backward-compatible factory without AccountData.
     */
    public static AccountOpenedEvent of(String accountId, String userId, Money initialBalance, EventMetadata metadata) {
        return new AccountOpenedEvent(accountId, userId, initialBalance, metadata, null);
    }
}
