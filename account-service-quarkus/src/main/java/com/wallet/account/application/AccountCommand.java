package com.wallet.account.application;

import java.math.BigDecimal;

import com.wallet.shared.money.Money;

/**
 * Commands for account operations.
 */
public sealed interface AccountCommand {

    record OpenAccount(String userId, Money initialBalance, String requestId) implements AccountCommand {}
    record Deposit(String accountId, BigDecimal amount, String currency, String requestId) implements AccountCommand {}
    record Withdraw(String accountId, BigDecimal amount, String currency, String requestId) implements AccountCommand {}
}
