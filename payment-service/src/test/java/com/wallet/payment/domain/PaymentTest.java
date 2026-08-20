package com.wallet.payment.domain;

import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Nested
    @DisplayName("money() factory")
    class MoneyFactory {

        @Test
        @DisplayName("Rehidrata Money desde amount + currency")
        void money_rehidrataValueObject() {
            Payment payment = new Payment();
            payment.setAmount(new BigDecimal("99.99"));
            payment.setCurrency("EUR");

            Money money = payment.money();

            assertThat(money.amount()).isEqualByComparingTo("99.99");
            assertThat(money.currency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Propaga validación de Money: amount inválido lanza excepción")
        void money_propagaValidacionAmount() {
            Payment payment = new Payment();
            payment.setAmount(BigDecimal.ZERO);
            payment.setCurrency("USD");

            assertThatThrownBy(payment::money)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
        }

        @Test
        @DisplayName("Propaga validación de Money: currency inválido lanza excepción")
        void money_propagaValidacionCurrency() {
            Payment payment = new Payment();
            payment.setAmount(new BigDecimal("10"));
            payment.setCurrency("usd");

            assertThatThrownBy(payment::money)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO-4217");
        }
    }

    @Nested
    @DisplayName("Campos básicos")
    class CamposBasicos {

        @Test
        @DisplayName("Lombok @Getter expone todos los campos del aggregate")
        void getters_expuestos() {
            Instant now = Instant.parse("2026-01-15T10:00:00Z");
            Payment payment = new Payment();
            payment.setId("pay-1");
            payment.setUserId("user-1");
            payment.setAmount(new BigDecimal("10"));
            payment.setCurrency("USD");
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setIdempotencyKey("idem-1");
            payment.setCreatedAt(now);
            payment.setUpdatedAt(now);
            payment.setVersion(3L);

            assertThat(payment.getId()).isEqualTo("pay-1");
            assertThat(payment.getUserId()).isEqualTo("user-1");
            assertThat(payment.getAmount()).isEqualByComparingTo("10");
            assertThat(payment.getCurrency()).isEqualTo("USD");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getIdempotencyKey()).isEqualTo("idem-1");
            assertThat(payment.getCreatedAt()).isEqualTo(now);
            assertThat(payment.getUpdatedAt()).isEqualTo(now);
            assertThat(payment.getVersion()).isEqualTo(3L);
        }
    }
}
