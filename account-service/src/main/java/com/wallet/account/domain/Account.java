package com.wallet.account.domain;

import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.money.Money;

import java.math.BigDecimal;

/**
 * Account aggregate — reconstituted from the event stream, NOT a JPA entity.
 *
 * Lifecycle:
 *   empty constructor → fold events → state emerges.
 *
 * Why fold events instead of updating columns:
 * - Auditability is free: every state change is a stored event with timestamp + correlationId.
 * - Schema evolution is easier: new event variants are added without rewriting old rows.
 * - CQRS read model stays trivially consistent — projection = fold of the same events.
 *
 * Trade-off documented in README: we trade write-side CPU (replay on load) for these gains.
 * For very long-lived aggregates we'd add periodic snapshots — see TODO.
 */
public class Account {

    public enum Status { OPEN, CLOSED }

    private String accountId;
    private String userId;
    private Money balance;
    private Status status;
    private long version;

    /** Used by the event store when reconstructing. */
    public Account() { }

    /**
     * Apply an event to the in-memory aggregate.
     * The pattern matching is exhaustive — sealed interface compile-time guarantee.
     */
    public void apply(AccountEvent event) {
        switch (event) {
            case AccountOpenedEvent e  -> applyOpened(e);
            case MoneyDepositedEvent e -> applyDeposited(e);
            case MoneyWithdrawnEvent e -> applyWithdrawn(e);
        }
    }

    private void applyOpened(AccountOpenedEvent e) {
        this.accountId = e.accountId();
        this.userId = e.userId();
        this.balance = e.initialBalance();
        this.status = Status.OPEN;
        this.version++;
    }

    private void applyDeposited(MoneyDepositedEvent e) {
        ensureSameAccount(e.accountId());
        this.balance = e.newBalance();
        this.version++;
    }

    private void applyWithdrawn(MoneyWithdrawnEvent e) {
        ensureSameAccount(e.accountId());
        this.balance = e.newBalance();
        this.version++;
    }

    private void ensureSameAccount(String otherId) {
        if (this.accountId == null) {
            throw new IllegalStateException("cannot apply event before AccountOpened");
        }
        if (!this.accountId.equals(otherId)) {
            throw new IllegalArgumentException(
                "event accountId " + otherId + " does not match aggregate " + this.accountId);
        }
    }

    // ---- Domain operations (used by command service before persisting a new event) ----

    /**
     * Validates a candidate withdrawal: balance must be ≥ amount, same currency.
     * @throws InsufficientFundsException when balance is too low.
     */
    public void assertCanWithdraw(Money amount) {
        if (status != Status.OPEN) {
            throw new IllegalStateException("account is not OPEN: " + status);
        }
        if (!balance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("currency mismatch");
        }
        if (balance.amount().compareTo(amount.amount()) < 0) {
            throw new com.wallet.account.domain.exception.InsufficientFundsException(
                accountId, balance.amount(), amount.amount());
        }
    }

    public Money nextBalanceAfterDeposit(Money amount) {
        ensureSameCurrency(amount);
        return new Money(balance.amount().add(amount.amount()), balance.currency());
    }

    public Money nextBalanceAfterWithdrawal(Money amount) {
        ensureSameCurrency(amount);
        return new Money(balance.amount().subtract(amount.amount()), balance.currency());
    }

    private void ensureSameCurrency(Money amount) {
        if (!balance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }

    // ---- Accessors ----

    public String accountId()  { return accountId; }
    public String userId()     { return userId; }
    public Money balance()     { return balance; }
    public Status status()     { return status; }
    public long version()      { return version; }
}
