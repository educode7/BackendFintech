package com.wallet.account.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

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

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String balanceCurrency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // Identification
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "account_type", length = 30)
    private String accountType;

    @Column(name = "cci", length = 20)
    private String cci;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "swift_bic", length = 11)
    private String swiftBic;

    // Holder info
    @Column(name = "holder_name", length = 120)
    private String holderName;

    @Column(name = "holder_document_type", length = 30)
    private String holderDocumentType;

    @Column(name = "holder_document_number", length = 20)
    private String holderDocumentNumber;

    @Column(name = "holder_email", length = 255)
    private String holderEmail;

    @Column(name = "holder_phone", length = 20)
    private String holderPhone;

    // Banking
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_currency", length = 3)
    private String accountCurrency;

    @Column(name = "country", length = 2)
    private String country;

    // Balance details
    @Column(name = "available_amount", precision = 19, scale = 4)
    private BigDecimal availableAmount;

    @Column(name = "available_amount_currency", length = 3)
    private String availableAmountCurrency;

    @Column(name = "hold_amount", precision = 19, scale = 4)
    private BigDecimal holdAmount;

    @Column(name = "hold_amount_currency", length = 3)
    private String holdAmountCurrency;

    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    private BigDecimal overdraftLimit;

    @Column(name = "overdraft_limit_currency", length = 3)
    private String overdraftLimitCurrency;

    // Limits
    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "daily_limit_currency", length = 3)
    private String dailyLimitCurrency;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "monthly_limit_currency", length = 3)
    private String monthlyLimitCurrency;

    @Column(name = "single_transaction_limit", precision = 19, scale = 4)
    private BigDecimal singleTransactionLimit;

    @Column(name = "single_transaction_limit_currency", length = 3)
    private String singleTransactionLimitCurrency;

    // Timestamps
    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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
        // Identification
        entity.accountNumber = view.accountNumber();
        entity.accountType = view.accountType();
        entity.cci = view.cci();
        entity.iban = view.iban();
        entity.swiftBic = view.swiftBic();
        // Holder info
        entity.holderName = view.holderName();
        entity.holderDocumentType = view.holderDocumentType();
        entity.holderDocumentNumber = view.holderDocumentNumber();
        entity.holderEmail = view.holderEmail();
        entity.holderPhone = view.holderPhone();
        // Banking
        entity.bankCode = view.bankCode();
        entity.bankName = view.bankName();
        entity.accountCurrency = view.currency();
        entity.country = view.country();
        // Balance details
        entity.availableAmount = view.availableAmount();
        entity.availableAmountCurrency = view.availableAmountCurrency();
        entity.holdAmount = view.holdAmount();
        entity.holdAmountCurrency = view.holdAmountCurrency();
        entity.overdraftLimit = view.overdraftLimit();
        entity.overdraftLimitCurrency = view.overdraftLimitCurrency();
        // Limits
        entity.dailyLimit = view.dailyLimit();
        entity.dailyLimitCurrency = view.dailyLimitCurrency();
        entity.monthlyLimit = view.monthlyLimit();
        entity.monthlyLimitCurrency = view.monthlyLimitCurrency();
        entity.singleTransactionLimit = view.singleTransactionLimit();
        entity.singleTransactionLimitCurrency = view.singleTransactionLimitCurrency();
        // Timestamps
        entity.activatedAt = view.activatedAt();
        entity.closedAt = view.closedAt();
        return entity;
    }

    public com.wallet.account.domain.AccountView toDomain() {
        return com.wallet.account.domain.AccountView.of(
                accountId, userId, balanceAmount, balanceCurrency, status, version, lastUpdated,
                accountNumber, accountType, cci, iban, swiftBic,
                holderName, holderDocumentType, holderDocumentNumber, holderEmail, holderPhone,
                bankCode, bankName, accountCurrency, country,
                availableAmount, availableAmountCurrency,
                holdAmount, holdAmountCurrency,
                overdraftLimit, overdraftLimitCurrency,
                dailyLimit, dailyLimitCurrency,
                monthlyLimit, monthlyLimitCurrency,
                singleTransactionLimit, singleTransactionLimitCurrency,
                activatedAt, closedAt);
    }

    // --- Getters ---

    public String getAccountId() { return accountId; }
    public String getUserId() { return userId; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public String getBalanceCurrency() { return balanceCurrency; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public String getCci() { return cci; }
    public String getIban() { return iban; }
    public String getSwiftBic() { return swiftBic; }
    public String getHolderName() { return holderName; }
    public String getHolderDocumentType() { return holderDocumentType; }
    public String getHolderDocumentNumber() { return holderDocumentNumber; }
    public String getHolderEmail() { return holderEmail; }
    public String getHolderPhone() { return holderPhone; }
    public String getBankCode() { return bankCode; }
    public String getBankName() { return bankName; }
    public String getAccountCurrency() { return accountCurrency; }
    public String getCountry() { return country; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public String getAvailableAmountCurrency() { return availableAmountCurrency; }
    public BigDecimal getHoldAmount() { return holdAmount; }
    public String getHoldAmountCurrency() { return holdAmountCurrency; }
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public String getOverdraftLimitCurrency() { return overdraftLimitCurrency; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public String getDailyLimitCurrency() { return dailyLimitCurrency; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public String getMonthlyLimitCurrency() { return monthlyLimitCurrency; }
    public BigDecimal getSingleTransactionLimit() { return singleTransactionLimit; }
    public String getSingleTransactionLimitCurrency() { return singleTransactionLimitCurrency; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getClosedAt() { return closedAt; }
}
