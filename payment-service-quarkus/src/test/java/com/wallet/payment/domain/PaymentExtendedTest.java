package com.wallet.payment.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.wallet.shared.money.Money;

@DisplayName("Payment Domain - Extended")
class PaymentExtendedTest {

    @Nested
    @DisplayName("Status enum")
    class StatusEnum {

        @Test
        @DisplayName("COMPLETED and FAILED should be terminal")
        void terminalStates() {
            assertTrue(Payment.Status.COMPLETED.isTerminal());
            assertTrue(Payment.Status.FAILED.isTerminal());
            assertFalse(Payment.Status.PENDING.isTerminal());
            assertFalse(Payment.Status.PROCESSING.isTerminal());
        }

        @Test
        @DisplayName("PENDING can transition to PROCESSING via startProcessing()")
        void pendingToProcessing() {
            Payment p = Payment.create("p1", "acc-1", "u1", new Money(BigDecimal.TEN, "USD"), "k1");
            assertEquals(Payment.Status.PENDING, p.status());
            Payment processing = p.startProcessing();
            assertEquals(Payment.Status.PROCESSING, processing.status());
        }

        @Test
        @DisplayName("PROCESSING can transition to COMPLETED or FAILED")
        void processingTransitions() {
            Payment p = Payment.create("p1", "acc-1", "u1", new Money(BigDecimal.TEN, "USD"), "k1")
                    .startProcessing();
            assertEquals(Payment.Status.COMPLETED, p.complete().status());
            assertEquals(Payment.Status.FAILED, p.fail().status());
        }

        @Test
        @DisplayName("Terminal states cannot transition")
        void terminalNoTransitions() {
            Payment completed = Payment.create("p1", "acc-1", "u1", new Money(BigDecimal.TEN, "USD"), "k1")
                    .startProcessing().complete();
            assertThrows(IllegalStateException.class, completed::startProcessing);
            assertThrows(IllegalStateException.class, completed::complete);
            assertThrows(IllegalStateException.class, completed::fail);

            Payment failed = Payment.create("p2", "acc-2", "u1", new Money(BigDecimal.TEN, "USD"), "k1")
                    .startProcessing().fail();
            assertThrows(IllegalStateException.class, failed::startProcessing);
            assertThrows(IllegalStateException.class, failed::complete);
            assertThrows(IllegalStateException.class, failed::fail);
        }
    }

    @Nested
    @DisplayName("Amount validation")
    class AmountValidation {

        @Test
        @DisplayName("should accept positive amounts with many decimals")
        void manyDecimals() {
            Money amount = new Money(new BigDecimal("0.00000001"), "USD");
            Payment payment = Payment.create("pay-001", "acc-001", "user-001", amount, "key-001");
            assertEquals(amount, payment.amount());
        }

        @Test
        @DisplayName("should accept large amounts")
        void largeAmount() {
            Money amount = new Money(new BigDecimal("999999999999.99"), "USD");
            Payment payment = Payment.create("pay-001", "acc-001", "user-001", amount, "key-001");
            assertEquals(amount, payment.amount());
        }

        @Test
        @DisplayName("should reject null amount")
        void nullAmount() {
            assertThrows(NullPointerException.class,
                    () -> Payment.create("pay-001", "acc-001", "user-001", null, "key-001"));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("payments with same ID should be equal")
        void sameId() {
            Payment p1 = Payment.create("pay-001", "acc-001", "user-001", new Money(BigDecimal.TEN, "USD"), "key-001");
            Payment p2 = Payment.create("pay-001", "acc-002", "user-002", new Money(BigDecimal.ONE, "EUR"), "key-002");
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("payments with different IDs should not be equal")
        void differentId() {
            Payment p1 = Payment.create("pay-001", "acc-001", "user-001", new Money(BigDecimal.TEN, "USD"), "key-001");
            Payment p2 = Payment.create("pay-002", "acc-001", "user-001", new Money(BigDecimal.TEN, "USD"), "key-001");
            assertNotEquals(p1, p2);
        }
    }
}
