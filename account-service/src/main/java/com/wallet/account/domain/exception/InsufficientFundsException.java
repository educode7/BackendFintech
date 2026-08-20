package com.wallet.account.domain.exception;

/**
 * Domain-level rejection: withdrawal amount > current balance.
 * Mapped to HTTP 422 Unprocessable Entity at the web layer.
 */
public class InsufficientFundsException extends RuntimeException {

    private final String accountId;
    private final java.math.BigDecimal balance;
    private final java.math.BigDecimal requested;

    public InsufficientFundsException(String accountId, java.math.BigDecimal balance, java.math.BigDecimal requested) {
        super("insufficient funds on account " + accountId + ": balance=" + balance + ", requested=" + requested);
        this.accountId = accountId;
        this.balance = balance;
        this.requested = requested;
    }

    public String accountId() { return accountId; }
    public java.math.BigDecimal balance() { return balance; }
    public java.math.BigDecimal requested() { return requested; }
}
