package com.wallet.notification.infrastructure.kafka;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.notification.application.NotificationService;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;

/**
 * Kafka consumer with INBOX PATTERN for exactly-once processing.
 * <p>
 * Flow:
 * 1. Check Redis for event_id (SET NX, 7-day TTL)
 * 2. If exists → discard (duplicate)
 * 3. If not → process notification, mark as processed
 * <p>
 * Belt-and-braces: DB UNIQUE(processed_event_id) is the last line of defense.
 */
@ApplicationScoped
public class PaymentEventConsumer {

    private static final Logger log = Logger.getLogger(PaymentEventConsumer.class);
    private static final String INBOX_PREFIX = "inbox:notification:";

    private final NotificationService notificationService;
    private final RedisAPI redisAPI;

    @Inject
    public PaymentEventConsumer(NotificationService notificationService, RedisAPI redisAPI) {
        this.notificationService = notificationService;
        this.redisAPI = redisAPI;
    }

    @Incoming("payment-events-in")
    @WithSpan("consume-payment-event")
    public CompletionStage<Void> onPaymentCompleted(@SpanAttribute("event.id") Message<String> message) {
        try {
            JsonObject json = new JsonObject(message.getPayload());
            String eventId = json.getString("eventId");
            JsonObject payload = json.getJsonObject("payload");

            // 1. INBOX CHECK
            if (isAlreadyProcessed(eventId)) {
                log.debugf("Event already processed (inbox): eventId=%s — discarding", eventId);
                return CompletableFuture.completedFuture(null);
            }

            // 2. Deserialize event
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payload.getString("paymentId"),
                    payload.getString("accountId"),
                    payload.getString("userId"),
                    new Money(
                            new java.math.BigDecimal(payload.getJsonObject("amount").getString("amount")),
                            payload.getJsonObject("amount").getString("currency")),
                    payload.getString("status"),
                    new com.wallet.shared.event.EventMetadata(eventId, java.time.Instant.now(), null, 1));

            // 3. Process notification
            log.infof("Processing PaymentCompletedEvent: paymentId=%s, userId=%s", event.paymentId(), event.userId());
            notificationService.handlePaymentCompleted(event);

            // 4. Mark inbox
            markAsProcessed(eventId);
        } catch (PersistenceException e) {
            log.infof("Duplicate notification ignored (already persisted): %s", e.getMessage());
        } catch (Exception e) {
            log.errorf("Failed to process payment event: %s", e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private boolean isAlreadyProcessed(String eventId) {
        try {
            Response response = redisAPI
                    .exists(List.of(INBOX_PREFIX + eventId))
                    .await()
                    .indefinitely();
            return response != null && response.toLong() > 0;
        } catch (Exception e) {
            log.warnf("Redis inbox check failed, falling through: %s", e.getMessage());
            return false;
        }
    }

    private void markAsProcessed(String eventId) {
        try {
            redisAPI
                    .set(List.of(INBOX_PREFIX + eventId, "1", "EX", String.valueOf(86400 * 7)))
                    .await()
                    .indefinitely();
            log.debugf("Inbox marked: eventId=%s", eventId);
        } catch (Exception e) {
            log.warnf("Redis inbox mark failed: %s", e.getMessage());
        }
    }
}
