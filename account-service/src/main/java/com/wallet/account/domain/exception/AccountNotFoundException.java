package com.wallet.account.domain.exception;

/**
 * Aggregate (account) doesn't exist in the event store.
 * Mapped to HTTP 404 at the web layer.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("account not found: " + accountId);
    }
}
