package com.wallet.account.domain.exception;

/**
 * Account not found by the given ID.
 */
public final class AccountNotFoundException extends RuntimeException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("account not found: " + accountId);
        this.accountId = accountId;
    }

    public String accountId() { return accountId; }
}
