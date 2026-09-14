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
        Instant lastUpdated
) {
    public static AccountResponse from(AccountView view) {
        return new AccountResponse(
                view.accountId(), view.userId(),
                view.balanceAmount(), view.balanceCurrency(),
                view.status(), view.version(), view.lastUpdated());
    }

    public static AccountResponse fromDomain(Account account) {
        return new AccountResponse(
                account.accountId(), account.userId(),
                account.balance().amount(), account.balance().currency(),
                account.status().name(), account.version(), Instant.now());
    }
}
