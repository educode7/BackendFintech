package com.wallet.payment.infrastructure.kafka;

import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.kafka.KafkaTopics;
import com.wallet.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Publishes payment domain events to Kafka.
 *
 * Why we attach headers explicitly (vs. just JSON body):
 * - Routers / consumers can filter on event-type without parsing the body.
 * - Correlation ID propagates across service boundaries without consumers
 *   needing to know the body's shape.
 *
 * Kafka producer config (acks=all, idempotence=true, snappy) lives in
 * application.yml so it's visible to ops.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(PaymentCompletedEvent event) {
        String payload = JsonUtil.toJson(event);
        Message<String> message = MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, KafkaTopics.PAYMENT_EVENTS)
            .setHeader(KafkaHeaders.KEY, event.paymentId())
            .setHeader(KafkaTopics.HEADER_EVENT_TYPE, "PaymentCompleted")
            .setHeader(KafkaTopics.HEADER_EVENT_ID, event.metadata().eventId())
            .setHeader(KafkaTopics.HEADER_CORRELATION_ID, event.metadata().correlationId())
            .build();
        kafkaTemplate.send(message);
        log.info("payment event published paymentId={} eventId={} correlationId={}",
            event.paymentId(),
            event.metadata().eventId(),
            event.metadata().correlationId());
    }
}
