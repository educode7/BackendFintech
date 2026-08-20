package com.wallet.account.infrastructure.persistence;

import com.wallet.account.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Hot backup of the CQRS read model — written by the projection service
 * alongside Redis. Used as a fallback if Redis is cold or evicted.
 *
 * Why both Redis and Postgres for the read model:
 * - Redis: sub-millisecond reads, no SQL overhead.
 * - Postgres: durable, so we can rebuild Redis from it without replaying the event store.
 */
@Entity
@Table(name = "account_view")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class AccountViewEntity {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String balanceCurrency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
