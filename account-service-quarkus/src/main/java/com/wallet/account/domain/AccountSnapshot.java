package com.wallet.account.domain;

import java.math.BigDecimal;

import com.wallet.shared.money.Money;

/**
 * Aggregate snapshot — captures Account state at a specific version
 * to avoid replaying the full event stream on every load.
 */
public record AccountSnapshot(
        String accountId,
        String userId,
        BigDecimal balanceAmount,
        String balanceCurrency,
        String status,
        long version
) {
    public AccountSnapshot {
        if (accountId == null) throw new NullPointerException("accountId");
        if (userId == null) throw new NullPointerException("userId");
        if (balanceAmount == null) throw new NullPointerException("balanceAmount");
        if (balanceCurrency == null) throw new NullPointerException("balanceCurrency");
        if (status == null) throw new NullPointerException("status");
    }

    public Money balance() {
        return new Money(balanceAmount, balanceCurrency);
    }

    /**
     * Build a snapshot from the current aggregate state.
     */
    public static AccountSnapshot from(Account account) {
        return new AccountSnapshot(
                account.accountId(),
                account.userId(),
                account.balance().amount(),
                account.balance().currency(),
                account.status().name(),
                account.version()
        );
    }
}
