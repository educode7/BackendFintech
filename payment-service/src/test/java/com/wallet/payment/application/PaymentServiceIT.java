package com.wallet.payment.application;

import com.wallet.payment.testsupport.AbstractIntegrationTest;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private com.wallet.payment.infrastructure.persistence.PaymentJpaRepository repository;

    @Test
    @DisplayName("Procesar un pago persiste fila y publica PaymentCompletedEvent en Kafka")
    void procesoPago_exitoso_persisteYPublica() {
        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
            "user-it-1",
            new com.wallet.shared.money.Money(new java.math.BigDecimal("123.45"), "USD"),
            "idem-it-1"
        );

        AtomicReference<PaymentCompletedEvent> consumed = new AtomicReference<>();
        try (KafkaConsumer<String, String> consumer = buildConsumer("it-success-" + System.nanoTime())) {
            consumer.subscribe(List.of(KafkaTopics.PAYMENT_EVENTS));

            PaymentResponse response = commandService.process(cmd);

            Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    for (var record : records) {
                        if (record.value().contains(response.id())) {
                            consumed.set(com.wallet.shared.util.JsonUtil.fromJson(
                                record.value(), PaymentCompletedEvent.class));
                            return;
                        }
                    }
                    throw new AssertionError("Kafka no recibió el evento del pago " + response.id());
                });

            assertThat(consumed.get().paymentId()).isEqualTo(response.id());
            assertThat(consumed.get().userId()).isEqualTo("user-it-1");
            assertThat(consumed.get().amount().amount()).isEqualByComparingTo("123.45");
            assertThat(consumed.get().status()).isEqualTo("COMPLETED");
        }

        assertThat(repository.findById(consumed.get().paymentId()))
            .as("la fila debe estar persistida en la DB")
            .isPresent();
    }

    @Test
    @DisplayName("Misma idempotency key retorna el mismo payment y no crea fila nueva")
    void procesoPago_idempotency_segundaLlamada_devuelveMismoPago() {
        ProcessPaymentCommand cmd = new ProcessPaymentCommand(
            "user-it-2",
            new com.wallet.shared.money.Money(new java.math.BigDecimal("50.00"), "EUR"),
            "idem-it-2"
        );

        PaymentResponse first = commandService.process(cmd);
        PaymentResponse second = commandService.process(cmd);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.findAll())
            .as("solo debe haber una fila para el idempotency key")
            .hasSize(1);
    }

    @Test
    @DisplayName("Distinta idempotency key crea pagos distintos")
    void procesoPago_idempotency_distintaKey_creaPagosDistintos() {
        ProcessPaymentCommand a = new ProcessPaymentCommand(
            "user-it-3",
            new com.wallet.shared.money.Money(new java.math.BigDecimal("10"), "USD"),
            "idem-it-3a"
        );
        ProcessPaymentCommand b = new ProcessPaymentCommand(
            "user-it-3",
            new com.wallet.shared.money.Money(new java.math.BigDecimal("20"), "USD"),
            "idem-it-3b"
        );

        PaymentResponse first = commandService.process(a);
        PaymentResponse second = commandService.process(b);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(repository.findAll()).hasSize(2);
    }

    private static KafkaConsumer<String, String> buildConsumer(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
