package com.wallet.shared.event;

import com.wallet.shared.money.Money;

/**
 * Data transferred when opening an account — carries all initial fields.
 * Lives in shared module so events can reference it without circular deps.
 */
public record AccountData(
    String accountNumber, AccountType accountType, String cci, String iban, String swiftBic,
    String holderName, HolderDocumentType holderDocumentType, String holderDocumentNumber,
    String holderEmail, String holderPhone,
    String bankCode, String bankName, String currency, String country,
    Money availableAmount, Money holdAmount, Money overdraftLimit,
    Money dailyLimit, Money monthlyLimit, Money singleTransactionLimit
) {}
