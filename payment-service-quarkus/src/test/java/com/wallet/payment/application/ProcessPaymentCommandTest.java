package com.wallet.payment.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.shared.money.Money;

@DisplayName("ProcessPaymentCommand")
class ProcessPaymentCommandTest {

    @Test
    @DisplayName("should create command with all fields")
    void shouldCreate() {
        Money amount = new Money(new BigDecimal("100.00"), "USD");
        ProcessPaymentCommand command = new ProcessPaymentCommand("acc-001", "user-001", amount, "key-001");

        assertEquals("acc-001", command.accountId());
        assertEquals("user-001", command.userId());
        assertEquals(amount, command.amount());
        assertEquals("key-001", command.idempotencyKey());
    }

    @Test
    @DisplayName("should reject null accountId")
    void nullAccountId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessPaymentCommand(null, "user-001", new Money(BigDecimal.TEN, "USD"), "key"));
    }

    @Test
    @DisplayName("should reject null userId")
    void nullUserId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessPaymentCommand("acc-001", null, new Money(BigDecimal.TEN, "USD"), "key"));
    }

    @Test
    @DisplayName("should reject null amount")
    void nullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessPaymentCommand("acc-001", "user-001", null, "key"));
    }

    @Test
    @DisplayName("should reject null idempotencyKey")
    void nullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessPaymentCommand("acc-001", "user-001", new Money(BigDecimal.TEN, "USD"), null));
    }
}
