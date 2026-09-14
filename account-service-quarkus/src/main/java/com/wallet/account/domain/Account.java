package com.wallet.account.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.wallet.shared.money.Money;

/**
 * Account aggregate root — Event Sourced.
 * <p>
 * Pure domain object. No JPA, no Quarkus, no frameworks.
 * State is derived by folding an immutable event stream.
 * <p>
 * Lifecycle:
 * 1. Open → AccountOpenedEvent appended
 * 2. Deposit → MoneyDepositedEvent appended
 * 3. Withdraw → MoneyWithdrawnEvent appended
 */
public final class Account {

    public enum Status { OPEN, CLOSED }

    private final String accountId;
    private final String userId;
    private Money balance;
    private Status status;
    private long version;

    // Transient — not persisted directly, derived from events
    private final List<com.wallet.shared.event.AccountEvent> pendingEvents = new ArrayList<>();

    private Account(String accountId, String userId, Money balance, Status status, long version) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    /**
     * Factory: create a new account (emits AccountOpenedEvent).
     */
    public static Account open(String accountId, String userId, Money initialBalance) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(initialBalance, "initialBalance");

        Account account = new Account(accountId, userId, initialBalance, Status.OPEN, 0);
        account.pendingEvents.add(new com.wallet.shared.event.AccountOpenedEvent(
                accountId, userId, initialBalance,
                com.wallet.shared.event.EventMetadata.create(null, 1)));
        return account;
    }

    /**
     * Reconstitute from event stream (event sourcing).
     */
    public static Account reconstitute(String accountId, String userId, Money balance,
                                       Status status, long version) {
        return new Account(accountId, userId, balance, status, version);
    }

    /**
     * Deposit funds. Emits MoneyDepositedEvent.
     *
     * @return this (mutates internal state, appends event)
     */
    public Account deposit(Money amount) {
        assertOpen();
        Objects.requireNonNull(amount, "amount");
        if (amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Money newBalance = balance.add(amount);
        version++;

        this.balance = newBalance;
        this.pendingEvents.add(new com.wallet.shared.event.MoneyDepositedEvent(
                accountId, amount, newBalance,
                com.wallet.shared.event.EventMetadata.create(null, (int) version)));
        return this;
    }

    /**
     * Withdraw funds. Emits MoneyWithdrawnEvent.
     * Fails if insufficient funds.
     *
     * @return this (mutates internal state, appends event)
     */
    public Account withdraw(Money amount) {
        assertOpen();
        Objects.requireNonNull(amount, "amount");
        if (amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (!balance.isGreaterThanOrEqual(amount)) {
            throw new com.wallet.account.domain.exception.InsufficientFundsException(
                    accountId, balance, amount);
        }

        Money newBalance = balance.subtract(amount);
        version++;

        this.balance = newBalance;
        this.pendingEvents.add(new com.wallet.shared.event.MoneyWithdrawnEvent(
                accountId, amount, newBalance,
                com.wallet.shared.event.EventMetadata.create(null, (int) version)));
        return this;
    }

    /**
     * Apply an event to rebuild state (for replay).
     */
    public void apply(com.wallet.shared.event.AccountEvent event) {
        switch (event) {
            case com.wallet.shared.event.AccountOpenedEvent e -> {
                this.balance = e.initialBalance();
            }
            case com.wallet.shared.event.MoneyDepositedEvent e -> {
                this.balance = e.newBalance();
                this.version = e.metadata().version();
            }
            case com.wallet.shared.event.MoneyWithdrawnEvent e -> {
                this.balance = e.newBalance();
                this.version = e.metadata().version();
            }
        };
    }

    /**
     * Get and clear pending events (for event store append).
     */
    public List<com.wallet.shared.event.AccountEvent> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    private void assertOpen() {
        if (status != Status.OPEN) {
            throw new IllegalStateException("Account is not open: " + status);
        }
    }

    // --- Getters ---

    public String accountId() { return accountId; }
    public String userId() { return userId; }
    public Money balance() { return balance; }
    public Status status() { return status; }
    public long version() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account a)) return false;
        return accountId.equals(a.accountId);
    }

    @Override
    public int hashCode() { return accountId.hashCode(); }

    @Override
    public String toString() {
        return "Account[id=%s, userId=%s, balance=%s, status=%s, version=%d]"
                .formatted(accountId, userId, balance, status, version);
    }
}
