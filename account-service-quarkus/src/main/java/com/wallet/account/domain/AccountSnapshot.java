package com.wallet.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.shared.money.Money;

/**
 * Aggregate snapshot — captures Account state at a specific version
 * to avoid replaying the full event stream on every load.
 */
public record AccountSnapshot(
        String accountId,
        String userId,
        BigDecimal balanceAmount,
        String balanceCurrency,
        String status,
        long version,
        // Identification
        String accountNumber,
        String accountType,
        // Holder info
        String holderName,
        String holderDocumentType,
        String holderDocumentNumber,
        // Banking
        String currency,
        String country,
        // Timestamps
        Instant activatedAt,
        Instant closedAt
) {
    public AccountSnapshot {
        if (accountId == null) throw new NullPointerException("accountId");
        if (userId == null) throw new NullPointerException("userId");
        if (balanceAmount == null) throw new NullPointerException("balanceAmount");
        if (balanceCurrency == null) throw new NullPointerException("balanceCurrency");
        if (status == null) throw new NullPointerException("status");
    }

    public Money balance() {
        return new Money(balanceAmount, balanceCurrency);
    }

    /**
     * Build a snapshot from the current aggregate state.
     */
    public static AccountSnapshot from(Account account) {
        return new AccountSnapshot(
                account.accountId(),
                account.userId(),
                account.balance().amount(),
                account.balance().currency(),
                account.status().name(),
                account.version(),
                // Identification
                account.accountNumber(),
                account.accountType() != null ? account.accountType().name() : null,
                // Holder info
                account.holderName(),
                account.holderDocumentType() != null ? account.holderDocumentType().name() : null,
                account.holderDocumentNumber(),
                // Banking
                account.currency(),
                account.country(),
                // Timestamps
                account.activatedAt(),
                account.closedAt()
        );
    }
}
