package com.wallet.account.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.wallet.shared.money.Money;
import com.wallet.shared.event.AccountData;
import com.wallet.shared.event.AccountType;
import com.wallet.shared.event.HolderDocumentType;

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

    // Identification
    private String accountNumber;
    private AccountType accountType;
    private String cci;
    private String iban;
    private String swiftBic;

    // Holder info
    private String holderName;
    private HolderDocumentType holderDocumentType;
    private String holderDocumentNumber;
    private String holderEmail;
    private String holderPhone;

    // Banking
    private String bankCode;
    private String bankName;
    private String currency;
    private String country;

    // Balance
    private Money balance;
    private Money availableAmount;
    private Money holdAmount;
    private Money overdraftLimit;

    // Limits
    private Money dailyLimit;
    private Money monthlyLimit;
    private Money singleTransactionLimit;

    // Status
    private Status status;
    private long version;

    // Timestamps
    private Instant activatedAt;
    private Instant closedAt;

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
     * Backward-compatible overload without AccountData.
     */
    public static Account open(String accountId, String userId, Money initialBalance) {
        return open(accountId, userId, initialBalance, null);
    }

    /**
     * Factory: create a new account (emits AccountOpenedEvent).
     */
    public static Account open(String accountId, String userId, Money initialBalance, AccountData data) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(initialBalance, "initialBalance");

        Account account = new Account(accountId, userId, initialBalance, Status.OPEN, 0);
        account.activatedAt = Instant.now();

        if (data != null) {
            applyAccountData(account, data);
            if (data.availableAmount() != null) account.availableAmount = data.availableAmount();
            else account.availableAmount = initialBalance;
            if (data.holdAmount() != null) account.holdAmount = data.holdAmount();
            else account.holdAmount = new Money(java.math.BigDecimal.ZERO, initialBalance.currency());
            account.overdraftLimit = data.overdraftLimit();
            account.dailyLimit = data.dailyLimit();
            account.monthlyLimit = data.monthlyLimit();
            account.singleTransactionLimit = data.singleTransactionLimit();
        } else {
            account.availableAmount = initialBalance;
            account.holdAmount = new Money(java.math.BigDecimal.ZERO, initialBalance.currency());
        }

        account.pendingEvents.add(new com.wallet.shared.event.AccountOpenedEvent(
                accountId, userId, initialBalance,
                com.wallet.shared.event.EventMetadata.create(null, 1),
                data));
        return account;
    }

    private static void applyAccountData(Account account, AccountData data) {
        account.accountNumber = data.accountNumber();
        account.accountType = data.accountType();
        account.cci = data.cci();
        account.iban = data.iban();
        account.swiftBic = data.swiftBic();
        account.holderName = data.holderName();
        account.holderDocumentType = data.holderDocumentType();
        account.holderDocumentNumber = data.holderDocumentNumber();
        account.holderEmail = data.holderEmail();
        account.holderPhone = data.holderPhone();
        account.bankCode = data.bankCode();
        account.bankName = data.bankName();
        account.currency = data.currency();
        account.country = data.country();
    }

    /**
     * Reconstitute from event stream (event sourcing).
     * Backward-compatible overload without AccountData.
     */
    public static Account reconstitute(String accountId, String userId, Money balance,
                                       Status status, long version) {
        return reconstitute(accountId, userId, balance, status, version, null);
    }

    /**
     * Reconstitute from event stream (event sourcing).
     */
    public static Account reconstitute(String accountId, String userId, Money balance,
                                       Status status, long version,
                                       AccountData data) {
        Account account = new Account(accountId, userId, balance, status, version);
        if (data != null) {
            applyAccountData(account, data);
            account.availableAmount = data.availableAmount();
            account.holdAmount = data.holdAmount();
            account.overdraftLimit = data.overdraftLimit();
            account.dailyLimit = data.dailyLimit();
            account.monthlyLimit = data.monthlyLimit();
            account.singleTransactionLimit = data.singleTransactionLimit();
        }
        return account;
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
                AccountData data = e.accountData();
                if (data != null) {
                    applyAccountData(this, data);
                    this.availableAmount = data.availableAmount() != null ? data.availableAmount() : e.initialBalance();
                    this.holdAmount = data.holdAmount() != null ? data.holdAmount() : new Money(java.math.BigDecimal.ZERO, e.initialBalance().currency());
                    this.overdraftLimit = data.overdraftLimit();
                    this.dailyLimit = data.dailyLimit();
                    this.monthlyLimit = data.monthlyLimit();
                    this.singleTransactionLimit = data.singleTransactionLimit();
                } else {
                    this.availableAmount = e.initialBalance();
                    this.holdAmount = new Money(java.math.BigDecimal.ZERO, e.initialBalance().currency());
                }
                this.activatedAt = e.metadata().occurredAt();
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

    public String accountNumber() { return accountNumber; }
    public AccountType accountType() { return accountType; }
    public String cci() { return cci; }
    public String iban() { return iban; }
    public String swiftBic() { return swiftBic; }
    public String holderName() { return holderName; }
    public HolderDocumentType holderDocumentType() { return holderDocumentType; }
    public String holderDocumentNumber() { return holderDocumentNumber; }
    public String holderEmail() { return holderEmail; }
    public String holderPhone() { return holderPhone; }
    public String bankCode() { return bankCode; }
    public String bankName() { return bankName; }
    public String currency() { return currency; }
    public String country() { return country; }
    public Money availableAmount() { return availableAmount; }
    public Money holdAmount() { return holdAmount; }
    public Money overdraftLimit() { return overdraftLimit; }
    public Money dailyLimit() { return dailyLimit; }
    public Money monthlyLimit() { return monthlyLimit; }
    public Money singleTransactionLimit() { return singleTransactionLimit; }
    public Instant activatedAt() { return activatedAt; }
    public Instant closedAt() { return closedAt; }

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
