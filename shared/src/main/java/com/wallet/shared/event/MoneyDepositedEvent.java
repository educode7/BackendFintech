package com.wallet.shared.event;

import com.wallet.shared.money.Money;

/**
 * Emitted by account-service after a successful deposit.
 * newBalance is the resulting balance after applying the event (snapshot for convenience).
 */
public record MoneyDepositedEvent(
    String accountId,
    Money amount,
    Money newBalance,
    EventMetadata metadata
) implements AccountEvent { }
