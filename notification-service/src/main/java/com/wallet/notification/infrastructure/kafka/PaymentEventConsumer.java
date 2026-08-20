package com.wallet.notification.infrastructure.kafka;

import com.wallet.notification.application.ConsumerIdempotencyService;
import com.wallet.notification.application.NotificationService;
import com.wallet.shared.context.CorrelationContext;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.kafka.KafkaTopics;
import com.wallet.shared.util.IdGenerator;
import com.wallet.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listens to {@code payment.events} and dispatches notifications.
 *
 * Idempotency flow:
 *  1. Receive event.
 *  2. Try to claim the eventId in Redis (SET NX).
 *  3. If claim fails → already processed → ack and move on.
 *  4. If claim succeeds → process, persist (UNIQUE constraint catches anything Redis missed), send.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ConsumerIdempotencyService idempotency;
    private final NotificationService notificationService;

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_EVENTS,
        groupId = KafkaTopics.GROUP_NOTIFICATION
    )
    public void onPaymentEvent(
        @Payload String payload,
        @Header(name = KafkaTopics.HEADER_CORRELATION_ID, required = false) String correlationId,
        @Header(name = KafkaTopics.HEADER_EVENT_ID, required = false) String eventId
    ) {
        String id = correlationId == null ? IdGenerator.newId() : correlationId;
        ScopedValue.where(CorrelationContext.CORRELATION_ID, id)
            .run(() -> processPaymentEvent(payload, eventId));
    }

    private void processPaymentEvent(String payload, String eventId) {
        try {
            PaymentCompletedEvent event = JsonUtil.fromJson(payload, PaymentCompletedEvent.class);
            String id = eventId != null ? eventId : event.metadata().eventId();
            if (!idempotency.tryClaim(id)) {
                log.info("skipping duplicate notification eventId={}", id);
                return;
            }
            notificationService.handlePaymentCompleted(event);
        } catch (Exception ex) {
            log.error("failed to handle payment event", ex);
        }
    }
}
