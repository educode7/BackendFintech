package com.wallet.account.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

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

    // Identification
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "account_type", length = 30)
    private String accountType;

    // Holder info
    @Column(name = "holder_name", length = 120)
    private String holderName;

    @Column(name = "holder_document_type", length = 30)
    private String holderDocumentType;

    @Column(name = "holder_document_number", length = 20)
    private String holderDocumentNumber;

    // Banking
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "country", length = 2)
    private String country;

    // Timestamps
    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public AccountSnapshotEntity() {}

    public static AccountSnapshotEntity fromDomain(com.wallet.account.domain.AccountSnapshot snapshot) {
        AccountSnapshotEntity entity = new AccountSnapshotEntity();
        entity.accountId = snapshot.accountId();
        entity.userId = snapshot.userId();
        entity.balanceAmount = snapshot.balanceAmount();
        entity.balanceCurrency = snapshot.balanceCurrency();
        entity.status = snapshot.status();
        entity.version = snapshot.version();
        // Identification
        entity.accountNumber = snapshot.accountNumber();
        entity.accountType = snapshot.accountType();
        // Holder info
        entity.holderName = snapshot.holderName();
        entity.holderDocumentType = snapshot.holderDocumentType();
        entity.holderDocumentNumber = snapshot.holderDocumentNumber();
        // Banking
        entity.currency = snapshot.currency();
        entity.country = snapshot.country();
        // Timestamps
        entity.activatedAt = snapshot.activatedAt();
        entity.closedAt = snapshot.closedAt();
        return entity;
    }

    public com.wallet.account.domain.AccountSnapshot toDomain() {
        return new com.wallet.account.domain.AccountSnapshot(
                accountId, userId, balanceAmount, balanceCurrency, status, version,
                accountNumber, accountType,
                holderName, holderDocumentType, holderDocumentNumber,
                currency, country,
                activatedAt, closedAt);
    }

    public String getAccountId() { return accountId; }
    public String getUserId() { return userId; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public String getBalanceCurrency() { return balanceCurrency; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public String getHolderName() { return holderName; }
    public String getHolderDocumentType() { return holderDocumentType; }
    public String getHolderDocumentNumber() { return holderDocumentNumber; }
    public String getCurrency() { return currency; }
    public String getCountry() { return country; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getClosedAt() { return closedAt; }
}
