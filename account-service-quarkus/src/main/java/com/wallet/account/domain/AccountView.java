package com.wallet.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.shared.money.Money;

/**
 * Account read model (CQRS projection).
 * Immutable record — represents the latest projected state.
 */
public record AccountView(
        String accountId,
        String userId,
        BigDecimal balanceAmount,
        String balanceCurrency,
        String status,
        long version,
        Instant lastUpdated
) {
    public AccountView {
        if (accountId == null) throw new NullPointerException("accountId");
        if (userId == null) throw new NullPointerException("userId");
        if (balanceAmount == null) throw new NullPointerException("balanceAmount");
        if (balanceCurrency == null) throw new NullPointerException("balanceCurrency");
        if (status == null) throw new NullPointerException("status");
    }

    public Money balance() {
        return new Money(balanceAmount, balanceCurrency);
    }

    public static AccountView fromDomain(Account account) {
        return new AccountView(
                account.accountId(),
                account.userId(),
                account.balance().amount(),
                account.balance().currency(),
                account.status().name(),
                account.version(),
                Instant.now()
        );
    }

    public static AccountView of(String accountId, String userId, BigDecimal balanceAmount,
                                  String balanceCurrency, String status, long version,
                                  Instant lastUpdated) {
        return new AccountView(accountId, userId, balanceAmount, balanceCurrency,
                status, version, lastUpdated);
    }
}
