package com.wallet.account.infrastructure.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the aggregate snapshot table.
 * One row per account — overwritten on each snapshot save.
 */
@Entity
@Table(name = "account_snapshot")
public class AccountSnapshotEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String balanceCurrency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version", nullable = false)
    private long version;

    public AccountSnapshotEntity() {}

    public static AccountSnapshotEntity fromDomain(com.wallet.account.domain.AccountSnapshot snapshot) {
        AccountSnapshotEntity entity = new AccountSnapshotEntity();
        entity.accountId = snapshot.accountId();
        entity.userId = snapshot.userId();
        entity.balanceAmount = snapshot.balanceAmount();
        entity.balanceCurrency = snapshot.balanceCurrency();
        entity.status = snapshot.status();
        entity.version = snapshot.version();
        return entity;
    }

    public com.wallet.account.domain.AccountSnapshot toDomain() {
        return new com.wallet.account.domain.AccountSnapshot(
                accountId, userId, balanceAmount, balanceCurrency, status, version);
    }

    public String getAccountId() { return accountId; }
    public String getUserId() { return userId; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public String getBalanceCurrency() { return balanceCurrency; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
}
