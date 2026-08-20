package com.wallet.notification.infrastructure.kafka;

import com.wallet.notification.infrastructure.persistence.NotificationJpaRepository;
import com.wallet.notification.testsupport.AbstractIntegrationTest;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationJpaRepository repository;

    @Test
    @DisplayName("Consume un PaymentCompletedEvent nuevo y crea una Notification")
    void consume_eventoNuevo_creaNotificacion() {
        String eventId = "evt-" + UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            "pay-it-1", "user-it-1",
            new Money(new BigDecimal("100.00"), "USD"),
            "COMPLETED",
            new EventMetadata(eventId, Instant.now(), "corr-it-1", 1)
        );

        sendToKafka("payment.events", "pay-it-1", eventId, "corr-it-1",
            com.wallet.shared.util.JsonUtil.toJson(event));

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                assertThat(repository.findById(eventId).orElseThrow().getUserId())
                    .isEqualTo("user-it-1");
            });
    }

    @Test
    @DisplayName("Consume el mismo eventId dos veces: idempotency evita notificación duplicada")
    void consume_mismoEvento_dosVeces_noEnviaDuplicado() {
        String eventId = "evt-dup-" + UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            "pay-it-2", "user-it-2",
            new Money(new BigDecimal("50.00"), "EUR"),
            "COMPLETED",
            new EventMetadata(eventId, Instant.now(), "corr-it-2", 1)
        );

        String payload = com.wallet.shared.util.JsonUtil.toJson(event);
        sendToKafka("payment.events", "pay-it-2", eventId, "corr-it-2", payload);
        sendToKafka("payment.events", "pay-it-2", eventId, "corr-it-2", payload);

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                long count = repository.findAll().stream()
                    .filter(n -> eventId.equals(n.getProcessedEventId()))
                    .count();
                assertThat(count).isEqualTo(1L);
            });
    }

    @Test
    @DisplayName("Consume evento con status distinto a COMPLETED: no crea notification")
    void consume_eventoNoCompleted_noCreaNotification() {
        String eventId = "evt-pending-" + UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            "pay-it-3", "user-it-3",
            new Money(new BigDecimal("10.00"), "USD"),
            "PENDING",
            new EventMetadata(eventId, Instant.now(), "corr-it-3", 1)
        );

        sendToKafka("payment.events", "pay-it-3", eventId, "corr-it-3",
            com.wallet.shared.util.JsonUtil.toJson(event));

        try { Thread.sleep(2000); } catch (InterruptedException ignored) { }

        long count = repository.findAll().stream()
            .filter(n -> eventId.equals(n.getProcessedEventId()))
            .count();
        assertThat(count)
            .as("eventos con status != COMPLETED deben ignorarse")
            .isZero();
    }

    private void sendToKafka(String topic, String key, String eventId, String correlationId, String payload) {
        org.springframework.messaging.support.MessageBuilder<String> builder =
            org.springframework.messaging.support.MessageBuilder.withPayload(payload)
                .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, topic)
                .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, key)
                .setHeader(com.wallet.shared.kafka.KafkaTopics.HEADER_EVENT_ID, eventId)
                .setHeader(com.wallet.shared.kafka.KafkaTopics.HEADER_CORRELATION_ID, correlationId);
        kafkaTemplate.send(builder.build());
    }
}
