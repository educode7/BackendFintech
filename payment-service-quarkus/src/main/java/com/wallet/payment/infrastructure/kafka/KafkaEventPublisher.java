package com.wallet.payment.infrastructure.kafka;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.payment.domain.EventPublisher;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

/**
 * Infrastructure adapter: EventPublisher implementation using Kafka via SmallRye Reactive Messaging.
 * <p>
 * Publishes PaymentCompletedEvent to the {@code payment.events} topic.
 * Uses Outbox pattern conceptually — event is published after DB commit.
 * The emitter is transactional: message is sent only after the enclosing transaction commits.
 */
@ApplicationScoped
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = Logger.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "payment.events";

    @Channel("payment-events-out")
    Emitter<String> emitter;

    @Override
    public void publishPaymentCompleted(String paymentId, String accountId, String userId, Money amount,
                                        String status, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        String eventType = "PaymentCompleted";

        JsonObject event = new JsonObject()
                .put("eventId", eventId)
                .put("eventType", eventType)
                .put("aggregateId", paymentId)
                .put("aggregateType", "Payment")
                .put("correlationId", correlationId)
                .put("payload", new JsonObject()
                        .put("paymentId", paymentId)
                        .put("accountId", accountId)
                        .put("userId", userId)
                        .put("amount", new JsonObject()
                                .put("amount", amount.amount().toPlainString())
                                .put("currency", amount.currency()))
                        .put("status", status));

        log.infof("Publishing %s event: paymentId=%s, key=%s", eventType, paymentId, paymentId);

        emitter.send(Message.of(event.encode())
                .addMetadata(io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.<String>builder()
                        .withTopic(TOPIC)
                        .withKey(paymentId)
                        .build()));
    }
}
