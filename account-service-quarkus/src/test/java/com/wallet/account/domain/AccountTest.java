package com.wallet.account.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.wallet.shared.money.Money;

@DisplayName("Account Aggregate")
class AccountTest {

    @Nested
    @DisplayName("open()")
    class Open {

        @Test
        @DisplayName("should create account in OPEN state with AccountOpenedEvent")
        void shouldCreateAccount() {
            Money balance = new Money(new BigDecimal("100.00"), "USD");
            Account account = Account.open("acc-001", "user-001", balance);

            assertEquals("acc-001", account.accountId());
            assertEquals("user-001", account.userId());
            assertEquals(balance, account.balance());
            assertEquals(Account.Status.OPEN, account.status());
            assertEquals(0, account.version());
            assertEquals(1, account.getPendingEvents().size());
            assertInstanceOf(com.wallet.shared.event.AccountOpenedEvent.class, account.getPendingEvents().getFirst());
        }

        @Test
        @DisplayName("should reject null arguments")
        void shouldRejectNulls() {
            assertThrows(NullPointerException.class, () -> Account.open(null, "user", new Money(BigDecimal.ONE, "USD")));
            assertThrows(NullPointerException.class, () -> Account.open("acc", null, new Money(BigDecimal.ONE, "USD")));
            assertThrows(NullPointerException.class, () -> Account.open("acc", "user", null));
        }
    }

    @Nested
    @DisplayName("deposit()")
    class Deposit {

        @Test
        @DisplayName("should increase balance and emit MoneyDepositedEvent")
        void shouldDeposit() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("100.00"), "USD"));
            account.clearPendingEvents();

            account.deposit(new Money(new BigDecimal("50.00"), "USD"));

            assertEquals(new BigDecimal("150.00"), account.balance().amount());
            assertEquals(1, account.version());
            assertEquals(1, account.getPendingEvents().size());
            assertInstanceOf(com.wallet.shared.event.MoneyDepositedEvent.class, account.getPendingEvents().getFirst());
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZero() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("100.00"), "USD"));
            assertThrows(IllegalArgumentException.class, () -> account.deposit(new Money(BigDecimal.ZERO, "USD")));
        }

        @Test
        @DisplayName("should reject deposit on closed account")
        void shouldRejectOnClosed() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("100.00"), "USD"));
            // Force close via reconstitute
            Account closed = Account.reconstitute("acc-001", "user-001",
                    new Money(new BigDecimal("100.00"), "USD"), Account.Status.CLOSED, 0);
            assertThrows(IllegalStateException.class, () -> closed.deposit(new Money(new BigDecimal("50.00"), "USD")));
        }
    }

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("should decrease balance and emit MoneyWithdrawnEvent")
        void shouldWithdraw() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("100.00"), "USD"));
            account.clearPendingEvents();

            account.withdraw(new Money(new BigDecimal("30.00"), "USD"));

            assertEquals(new BigDecimal("70.00"), account.balance().amount());
            assertEquals(1, account.version());
            assertInstanceOf(com.wallet.shared.event.MoneyWithdrawnEvent.class, account.getPendingEvents().getFirst());
        }

        @Test
        @DisplayName("should reject withdrawal exceeding balance")
        void shouldRejectInsufficientFunds() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("50.00"), "USD"));
            assertThrows(com.wallet.account.domain.exception.InsufficientFundsException.class,
                    () -> account.withdraw(new Money(new BigDecimal("100.00"), "USD")));
        }
    }

    @Nested
    @DisplayName("apply() — event replay")
    class Apply {

        @Test
        @DisplayName("should reconstitute state from events")
        void shouldReplayEvents() {
            Account account = Account.open("acc-001", "user-001", new Money(new BigDecimal("100.00"), "USD"));
            Money newBalance = account.balance().add(new Money(new BigDecimal("50.00"), "USD"));

            com.wallet.shared.event.MoneyDepositedEvent depositEvent =
                    new com.wallet.shared.event.MoneyDepositedEvent(
                            "acc-001",
                            new Money(new BigDecimal("50.00"), "USD"),
                            newBalance,
                            com.wallet.shared.event.EventMetadata.create("corr-001", 1));

            account.apply(depositEvent);

            assertEquals(newBalance, account.balance());
            assertEquals(1, account.version());
        }
    }
}
