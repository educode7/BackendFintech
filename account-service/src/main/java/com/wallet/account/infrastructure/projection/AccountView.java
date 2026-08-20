package com.wallet.account.infrastructure.projection;

import com.wallet.account.domain.Account;

import java.time.Instant;

/**
 * The CQRS read model. Lives in Redis (primary) and Postgres (backup).
 *
 * Why a record and not a JPA entity:
 * - It's a value-shaped projection — no identity beyond accountId, no relationships.
 * - Redis Hash serialisation is straightforward with primitive fields.
 */
public record AccountView(
    String accountId,
    String userId,
    java.math.BigDecimal balanceAmount,
    String balanceCurrency,
    Account.Status status,
    long version,
    Instant lastUpdated
) {

    public static AccountView fromDomain(Account account) {
        return new AccountView(
            account.accountId(),
            account.userId(),
            account.balance().amount(),
            account.balance().currency(),
            account.status(),
            account.version(),
            java.time.Instant.now()
        );
    }
}
