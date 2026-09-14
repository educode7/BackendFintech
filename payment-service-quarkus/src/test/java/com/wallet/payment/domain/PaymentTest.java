package com.wallet.payment.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.wallet.shared.money.Money;

@DisplayName("Payment Aggregate")
class PaymentTest {

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create payment in PENDING state")
        void shouldCreatePaymentInPendingState() {
            Money amount = new Money(new BigDecimal("25.50"), "USD");
            Payment payment = Payment.create("pay-001", "user-001", amount, "key-001");

            assertEquals("pay-001", payment.id());
            assertEquals("user-001", payment.userId());
            assertEquals(amount, payment.amount());
            assertEquals("key-001", payment.idempotencyKey());
            assertEquals(Payment.Status.PENDING, payment.status());
            assertEquals(0, payment.version());
            assertNotNull(payment.createdAt());
            assertNotNull(payment.updatedAt());
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.create("pay-001", "user-001", new Money(BigDecimal.ZERO, "USD"), "key-001"));
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.create("pay-001", "user-001", new Money(new BigDecimal("-10.00"), "USD"), "key-001"));
        }

        @Test
        @DisplayName("should reject null arguments")
        void shouldRejectNullArguments() {
            assertThrows(NullPointerException.class,
                    () -> Payment.create(null, "user-001", new Money(BigDecimal.ONE, "USD"), "key"));
            assertThrows(NullPointerException.class,
                    () -> Payment.create("pay-001", null, new Money(BigDecimal.ONE, "USD"), "key"));
            assertThrows(NullPointerException.class,
                    () -> Payment.create("pay-001", "user-001", null, "key"));
            assertThrows(NullPointerException.class,
                    () -> Payment.create("pay-001", "user-001", new Money(BigDecimal.ONE, "USD"), null));
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("PENDING -> PROCESSING -> COMPLETED")
        void shouldTransitionToCompleted() {
            Money amount = new Money(new BigDecimal("25.50"), "USD");
            Payment payment = Payment.create("pay-001", "user-001", amount, "key-001");

            Payment processing = payment.startProcessing();
            assertEquals(Payment.Status.PROCESSING, processing.status());
            assertEquals(1, processing.version());

            Payment completed = processing.complete();
            assertEquals(Payment.Status.COMPLETED, completed.status());
            assertEquals(2, completed.version());
            assertTrue(completed.status().isTerminal());
        }

        @Test
        @DisplayName("PENDING -> PROCESSING -> FAILED")
        void shouldTransitionToFailed() {
            Money amount = new Money(new BigDecimal("25.50"), "USD");
            Payment payment = Payment.create("pay-001", "user-001", amount, "key-001");

            Payment processing = payment.startProcessing();
            Payment failed = processing.fail();
            assertEquals(Payment.Status.FAILED, failed.status());
            assertTrue(failed.status().isTerminal());
        }

        @Test
        @DisplayName("should reject transition from PENDING directly to COMPLETED")
        void shouldRejectPendingToCompleted() {
            Money amount = new Money(new BigDecimal("25.50"), "USD");
            Payment payment = Payment.create("pay-001", "user-001", amount, "key-001");

            assertThrows(IllegalStateException.class, payment::complete);
        }

        @Test
        @DisplayName("should reject transition from terminal state")
        void shouldRejectTransitionFromTerminal() {
            Money amount = new Money(new BigDecimal("25.50"), "USD");
            Payment payment = Payment.create("pay-001", "user-001", amount, "key-001");
            Payment completed = payment.startProcessing().complete();

            assertThrows(IllegalStateException.class, completed::startProcessing);
            assertThrows(IllegalStateException.class, completed::complete);
            assertThrows(IllegalStateException.class, completed::fail);
        }
    }

    @Nested
    @DisplayName("of() — reconstitute from persistence")
    class Reconstitute {

        @Test
        @DisplayName("should reconstitute with correct values")
        void shouldReconstitute() {
            Money amount = new Money(new BigDecimal("100.00"), "EUR");
            Instant now = Instant.now();
            Payment payment = Payment.of("pay-001", "user-001", amount, "key-001",
                    Payment.Status.COMPLETED, 5, now, now);

            assertEquals("pay-001", payment.id());
            assertEquals(Payment.Status.COMPLETED, payment.status());
            assertEquals(5, payment.version());
        }
    }
}
