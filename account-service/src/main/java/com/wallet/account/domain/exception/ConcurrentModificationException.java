package com.wallet.account.domain.exception;

/**
 * Optimistic concurrency failure: two concurrent writers tried to append
 * the same {@code (aggregateId, version)} tuple to the event store.
 * After 3 retries the caller sees this — they can re-issue the command.
 */
public class ConcurrentModificationException extends RuntimeException {
    public ConcurrentModificationException(String accountId) {
        super("concurrent modification on account " + accountId + ", please retry");
    }
}
