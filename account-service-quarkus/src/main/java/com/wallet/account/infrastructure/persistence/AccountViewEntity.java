package com.wallet.account.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the CQRS read model (account_view).
 * Updated asynchronously via projection service.
 */
@Entity
@Table(name = "account_view")
public class AccountViewEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private java.math.BigDecimal balanceAmount;

    @Column(nullable = false, length = 3)
    private String balanceCurrency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private long version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public AccountViewEntity() {}

    public static AccountViewEntity fromDomain(com.wallet.account.domain.AccountView view) {
        AccountViewEntity entity = new AccountViewEntity();
        entity.accountId = view.accountId();
        entity.userId = view.userId();
        entity.balanceAmount = view.balanceAmount();
        entity.balanceCurrency = view.balanceCurrency();
        entity.status = view.status();
        entity.version = view.version();
        entity.lastUpdated = view.lastUpdated();
        return entity;
    }

    public com.wallet.account.domain.AccountView toDomain() {
        return com.wallet.account.domain.AccountView.of(
                accountId, userId, balanceAmount, balanceCurrency, status, version, lastUpdated);
    }

    // --- Getters ---

    public String getAccountId() { return accountId; }
    public String getUserId() { return userId; }
    public java.math.BigDecimal getBalanceAmount() { return balanceAmount; }
    public String getBalanceCurrency() { return balanceCurrency; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }
}
