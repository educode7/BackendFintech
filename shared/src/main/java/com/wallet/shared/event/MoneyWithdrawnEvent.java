package com.wallet.shared.event;

import com.wallet.shared.money.Money;

/**
 * Emitted by account-service after a successful withdrawal.
 */
public record MoneyWithdrawnEvent(
    String accountId,
    Money amount,
    Money newBalance,
    EventMetadata metadata
) implements AccountEvent { }
