package com.wallet.account.domain.exception;

/**
 * Optimistic concurrency conflict — event store version mismatch.
 */
public final class ConcurrentModificationException extends RuntimeException {

    private final String accountId;
    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentModificationException(String accountId, long expectedVersion, long actualVersion) {
        super("Concurrent modification on account %s: expected version %d, found %d"
                .formatted(accountId, expectedVersion, actualVersion));
        this.accountId = accountId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public String accountId() { return accountId; }
    public long expectedVersion() { return expectedVersion; }
    public long actualVersion() { return actualVersion; }
}
