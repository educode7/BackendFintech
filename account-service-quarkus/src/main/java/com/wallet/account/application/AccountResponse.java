package com.wallet.account.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.AccountView;

/**
 * Account read model DTO.
 */
public record AccountResponse(
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
    public static AccountResponse from(AccountView view) {
        return new AccountResponse(
                view.accountId(), view.userId(),
                view.balanceAmount(), view.balanceCurrency(),
                view.status(), view.version(), view.lastUpdated(),
                // Identification
                view.accountNumber(), view.accountType(),
                view.cci(), view.iban(), view.swiftBic(),
                // Holder info
                view.holderName(), view.holderDocumentType(),
                view.holderDocumentNumber(), view.holderEmail(), view.holderPhone(),
                // Banking
                view.bankCode(), view.bankName(), view.currency(), view.country(),
                // Balance details
                view.availableAmount(), view.availableAmountCurrency(),
                view.holdAmount(), view.holdAmountCurrency(),
                view.overdraftLimit(), view.overdraftLimitCurrency(),
                // Limits
                view.dailyLimit(), view.dailyLimitCurrency(),
                view.monthlyLimit(), view.monthlyLimitCurrency(),
                view.singleTransactionLimit(), view.singleTransactionLimitCurrency(),
                // Timestamps
                view.activatedAt(), view.closedAt());
    }

    public static AccountResponse fromDomain(Account account) {
        return new AccountResponse(
                account.accountId(), account.userId(),
                account.balance().amount(), account.balance().currency(),
                account.status().name(), account.version(), Instant.now(),
                // Identification
                account.accountNumber(),
                account.accountType() != null ? account.accountType().name() : null,
                account.cci(), account.iban(), account.swiftBic(),
                // Holder info
                account.holderName(),
                account.holderDocumentType() != null ? account.holderDocumentType().name() : null,
                account.holderDocumentNumber(), account.holderEmail(), account.holderPhone(),
                // Banking
                account.bankCode(), account.bankName(), account.currency(), account.country(),
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
                account.activatedAt(), account.closedAt());
    }
}
