package com.wallet.account.infrastructure.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.account.domain.EventPublisher;

import io.vertx.core.json.JsonObject;

/**
 * Infrastructure adapter: EventPublisher using Kafka via SmallRye Reactive Messaging.
 */
@ApplicationScoped
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = Logger.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "account.events";

    @Channel("account-events-out")
    Emitter<String> emitter;

    @Override
    public void publish(String eventType, String aggregateId, String payload, String correlationId) {
        String eventId = java.util.UUID.randomUUID().toString();

        JsonObject event = new JsonObject()
                .put("eventId", eventId)
                .put("eventType", eventType)
                .put("aggregateId", aggregateId)
                .put("aggregateType", "Account")
                .put("correlationId", correlationId)
                .put("payload", new JsonObject(payload));

        log.infof("Publishing %s event: aggregateId=%s", eventType, aggregateId);

        emitter.send(Message.of(event.encode())
                .addMetadata(io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.<String>builder().withKey(aggregateId).build()));
    }
}
