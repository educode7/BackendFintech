package com.wallet.account.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.shared.money.Money;

@DisplayName("AccountCommand")
class AccountCommandTest {

    @Test
    @DisplayName("OpenAccount should create command")
    void openAccount() {
        Money balance = new Money(new BigDecimal("100.00"), "USD");
        AccountCommand.OpenAccount cmd = new AccountCommand.OpenAccount("user-001", balance, "req-001");

        assertEquals("user-001", cmd.userId());
        assertEquals(balance, cmd.initialBalance());
        assertEquals("req-001", cmd.requestId());
    }

    @Test
    @DisplayName("Deposit should create command")
    void deposit() {
        AccountCommand.Deposit cmd = new AccountCommand.Deposit("acc-001", new BigDecimal("50.00"), "USD", "req-002");

        assertEquals("acc-001", cmd.accountId());
        assertEquals(new BigDecimal("50.00"), cmd.amount());
        assertEquals("USD", cmd.currency());
        assertEquals("req-002", cmd.requestId());
    }

    @Test
    @DisplayName("Withdraw should create command")
    void withdraw() {
        AccountCommand.Withdraw cmd = new AccountCommand.Withdraw("acc-001", new BigDecimal("25.00"), "EUR", "req-003");

        assertEquals("acc-001", cmd.accountId());
        assertEquals(new BigDecimal("25.00"), cmd.amount());
        assertEquals("EUR", cmd.currency());
    }
}
