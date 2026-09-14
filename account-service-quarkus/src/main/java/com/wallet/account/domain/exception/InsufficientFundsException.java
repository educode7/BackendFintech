package com.wallet.account.domain.exception;

import com.wallet.shared.money.Money;

/**
 * Withdrawal exceeds available balance.
 */
public final class InsufficientFundsException extends RuntimeException {

    private final String accountId;
    private final Money balance;
    private final Money requested;

    public InsufficientFundsException(String accountId, Money balance, Money requested) {
        super("Insufficient funds for account %s: available %s %s, requested %s %s"
                .formatted(accountId,
                        balance.amount(), balance.currency(),
                        requested.amount(), requested.currency()));
        this.accountId = accountId;
        this.balance = balance;
        this.requested = requested;
    }

    public String accountId() { return accountId; }
    public Money balance() { return balance; }
    public Money requested() { return requested; }
}
