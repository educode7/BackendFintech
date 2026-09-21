package com.wallet.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.shared.money.Money;

/**
 * Account read model (CQRS projection).
 * Immutable record — represents the latest projected state.
 */
public record AccountView(
        String accountId,
        String userId,
        BigDecimal balanceAmount,
        String balanceCurrency,
        String status,
        long version,
        Instant lastUpdated,
        // Identification
        String accountNumber,
        String accountType,
        String cci,
        String iban,
        String swiftBic,
        // Holder info
        String holderName,
        String holderDocumentType,
        String holderDocumentNumber,
        String holderEmail,
        String holderPhone,
        // Banking
        String bankCode,
        String bankName,
        String currency,
        String country,
        // Balance details
        BigDecimal availableAmount,
        String availableAmountCurrency,
        BigDecimal holdAmount,
        String holdAmountCurrency,
        BigDecimal overdraftLimit,
        String overdraftLimitCurrency,
        // Limits
        BigDecimal dailyLimit,
        String dailyLimitCurrency,
        BigDecimal monthlyLimit,
        String monthlyLimitCurrency,
        BigDecimal singleTransactionLimit,
        String singleTransactionLimitCurrency,
        // Timestamps
        Instant activatedAt,
        Instant closedAt
) {
    public AccountView {
        if (accountId == null) throw new NullPointerException("accountId");
        if (userId == null) throw new NullPointerException("userId");
        if (balanceAmount == null) throw new NullPointerException("balanceAmount");
        if (balanceCurrency == null) throw new NullPointerException("balanceCurrency");
        if (status == null) throw new NullPointerException("status");
    }

    public Money balance() {
        return new Money(balanceAmount, balanceCurrency);
    }

    public static AccountView fromDomain(Account account) {
        return new AccountView(
                account.accountId(),
                account.userId(),
                account.balance().amount(),
                account.balance().currency(),
                account.status().name(),
                account.version(),
                Instant.now(),
                // Identification
                account.accountNumber(),
                account.accountType() != null ? account.accountType().name() : null,
                account.cci(),
                account.iban(),
                account.swiftBic(),
                // Holder info
                account.holderName(),
                account.holderDocumentType() != null ? account.holderDocumentType().name() : null,
                account.holderDocumentNumber(),
                account.holderEmail(),
                account.holderPhone(),
                // Banking
                account.bankCode(),
                account.bankName(),
                account.currency(),
                account.country(),
                // Balance details
                account.availableAmount() != null ? account.availableAmount().amount() : null,
                account.availableAmount() != null ? account.availableAmount().currency() : null,
                account.holdAmount() != null ? account.holdAmount().amount() : null,
                account.holdAmount() != null ? account.holdAmount().currency() : null,
                account.overdraftLimit() != null ? account.overdraftLimit().amount() : null,
                account.overdraftLimit() != null ? account.overdraftLimit().currency() : null,
                // Limits
                account.dailyLimit() != null ? account.dailyLimit().amount() : null,
                account.dailyLimit() != null ? account.dailyLimit().currency() : null,
                account.monthlyLimit() != null ? account.monthlyLimit().amount() : null,
                account.monthlyLimit() != null ? account.monthlyLimit().currency() : null,
                account.singleTransactionLimit() != null ? account.singleTransactionLimit().amount() : null,
                account.singleTransactionLimit() != null ? account.singleTransactionLimit().currency() : null,
                // Timestamps
                account.activatedAt(),
                account.closedAt()
        );
    }

    public static AccountView of(String accountId, String userId, BigDecimal balanceAmount,
                                  String balanceCurrency, String status, long version,
                                  Instant lastUpdated,
                                  String accountNumber, String accountType, String cci, String iban, String swiftBic,
                                  String holderName, String holderDocumentType, String holderDocumentNumber,
                                  String holderEmail, String holderPhone,
                                  String bankCode, String bankName, String currency, String country,
                                  BigDecimal availableAmount, String availableAmountCurrency,
                                  BigDecimal holdAmount, String holdAmountCurrency,
                                  BigDecimal overdraftLimit, String overdraftLimitCurrency,
                                  BigDecimal dailyLimit, String dailyLimitCurrency,
                                  BigDecimal monthlyLimit, String monthlyLimitCurrency,
                                  BigDecimal singleTransactionLimit, String singleTransactionLimitCurrency,
                                  Instant activatedAt, Instant closedAt) {
        return new AccountView(accountId, userId, balanceAmount, balanceCurrency,
                status, version, lastUpdated,
                accountNumber, accountType, cci, iban, swiftBic,
                holderName, holderDocumentType, holderDocumentNumber, holderEmail, holderPhone,
                bankCode, bankName, currency, country,
                availableAmount, availableAmountCurrency,
                holdAmount, holdAmountCurrency,
                overdraftLimit, overdraftLimitCurrency,
                dailyLimit, dailyLimitCurrency,
                monthlyLimit, monthlyLimitCurrency,
                singleTransactionLimit, singleTransactionLimitCurrency,
                activatedAt, closedAt);
    }
}
