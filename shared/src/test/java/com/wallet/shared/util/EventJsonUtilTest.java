package com.wallet.shared.util;

import com.wallet.shared.event.AccountData;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventJsonUtilTest {

    private static final EventMetadata METADATA = new EventMetadata(
        "01926b3a-7c8a-7def-8000-000000000001",
        Instant.parse("2026-01-15T10:00:00Z"),
        "corr-test",
        1
    );

    @Nested
    @DisplayName("PaymentCompletedEvent")
    class PaymentCompleted {

        @Test
        @DisplayName("round-trip preserva todos los campos")
        void roundTrip() {
            PaymentCompletedEvent original = new PaymentCompletedEvent(
                "pay-1", "acc-1", "user-1",
                new Money(new BigDecimal("250.75"), "EUR"),
                "COMPLETED", METADATA
            );

            String json = JsonUtil.toJson(original);
            PaymentCompletedEvent parsed = JsonUtil.fromJson(json, PaymentCompletedEvent.class);

            assertThat(parsed).isEqualTo(original);
        }

        @Test
        @DisplayName("JSON usa nombres camelCase")
        void camelCase() {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                "pay-1", "acc-1", "user-1", new Money(new BigDecimal("10"), "USD"), "COMPLETED", METADATA
            );

            String json = JsonUtil.toJson(event);

            assertThat(json).contains("\"paymentId\":\"pay-1\"");
            assertThat(json).contains("\"userId\":\"user-1\"");
        }
    }

    @Nested
    @DisplayName("AccountOpenedEvent")
    class AccountOpened {

        @Test
        @DisplayName("round-trip preserva todos los campos")
        void roundTrip() {
            AccountOpenedEvent original = new AccountOpenedEvent(
                "acc-1", "user-1",
                new Money(new BigDecimal("0.01"), "USD"),
                METADATA,
                null
            );

            String json = JsonUtil.toJson(original);
            AccountOpenedEvent parsed = JsonUtil.fromJson(json, AccountOpenedEvent.class);

            assertThat(parsed).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("MoneyDepositedEvent")
    class MoneyDeposited {

        @Test
        @DisplayName("round-trip preserva todos los campos")
        void roundTrip() {
            MoneyDepositedEvent original = new MoneyDepositedEvent(
                "acc-1",
                new Money(new BigDecimal("100.00"), "USD"),
                new Money(new BigDecimal("150.00"), "USD"),
                METADATA
            );

            String json = JsonUtil.toJson(original);
            MoneyDepositedEvent parsed = JsonUtil.fromJson(json, MoneyDepositedEvent.class);

            assertThat(parsed).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("MoneyWithdrawnEvent")
    class MoneyWithdrawn {

        @Test
        @DisplayName("round-trip preserva todos los campos")
        void roundTrip() {
            MoneyWithdrawnEvent original = new MoneyWithdrawnEvent(
                "acc-1",
                new Money(new BigDecimal("25.00"), "USD"),
                new Money(new BigDecimal("75.00"), "USD"),
                METADATA
            );

            String json = JsonUtil.toJson(original);
            MoneyWithdrawnEvent parsed = JsonUtil.fromJson(json, MoneyWithdrawnEvent.class);

            assertThat(parsed).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Money serialización")
    class MoneyJson {

        @Test
        @DisplayName("Money embebido se serializa como 'amount currency'")
        void money_comoString() {
            MoneyDepositedEvent event = new MoneyDepositedEvent(
                "acc-1",
                new Money(new BigDecimal("100.00"), "USD"),
                new Money(new BigDecimal("200.00"), "USD"),
                METADATA
            );

            String json = JsonUtil.toJson(event);

            assertThat(json).contains("\"100.00 USD\"");
            assertThat(json).contains("\"200.00 USD\"");
        }
    }

    @Nested
    @DisplayName("EventMetadata")
    class Metadata {

        @Test
        @DisplayName("eventId es UUID v7 válido")
        void eventIdUuidValido() {
            String json = JsonUtil.toJson(METADATA);

            assertThat(json).contains("\"eventId\":\"01926b3a-7c8a-7def-8000-000000000001\"");
        }

        @Test
        @DisplayName("correlationId se preserva en el round-trip")
        void correlationIdSePreserva() {
            EventMetadata withCorr = new EventMetadata(
                "01926b3a-7c8a-7def-8000-000000000002",
                Instant.parse("2026-01-15T10:00:00Z"),
                "corr-abc",
                1
            );

            String json = JsonUtil.toJson(withCorr);
            EventMetadata parsed = JsonUtil.fromJson(json, EventMetadata.class);

            assertThat(parsed.correlationId()).isEqualTo("corr-abc");
        }
    }
}
