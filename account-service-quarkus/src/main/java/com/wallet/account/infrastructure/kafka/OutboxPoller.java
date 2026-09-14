package com.wallet.account.infrastructure.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.account.infrastructure.persistence.EventStoreEntity;
import com.wallet.account.infrastructure.persistence.JpaEventStore;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

/**
 * Polls the event store (transactional outbox) and publishes events to Kafka.
 * <p>
 * This is the ONLY component that publishes to Kafka for account events.
 * The use case writes events to the event store in the same transaction as business data,
 * and this poller picks up unpublished events and sends them.
 * <p>
 * Guarantees: at-least-once delivery. If publishing fails, the event
 * remains unpublished and will be retried on the next poll cycle.
 */
@ApplicationScoped
public class OutboxPoller {

    private static final Logger log = Logger.getLogger(OutboxPoller.class);
    private static final String TOPIC = "account.events";
    private static final int BATCH_SIZE = 50;

    private final JpaEventStore eventStore;

    @Channel("account-events-out")
    Emitter<String> emitter;

    @Inject
    public OutboxPoller(JpaEventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Poll unpublished events every 500ms and publish to Kafka.
     */
    @Scheduled(every = "500ms")
    void poll() {
        List<EventStoreEntity> events = eventStore.findUnpublished(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        log.debugf("Polling outbox: found %d unpublished events", events.size());

        List<UUID> publishedIds = new ArrayList<>();

        for (EventStoreEntity event : events) {
            try {
                // Build the event payload with metadata
                JsonObject eventJson = new JsonObject(event.getPayload());
                eventJson.put("eventId", UUID.randomUUID().toString());
                eventJson.put("eventType", event.getEventType());
                eventJson.put("aggregateId", event.getAggregateId());
                eventJson.put("aggregateType", "Account");
                eventJson.put("correlationId", event.getCorrelationId());

                OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                        .withTopic(TOPIC)
                        .withKey(event.getAggregateId())
                        .build();

                emitter.send(Message.of(eventJson.encode()).addMetadata(metadata));
                publishedIds.add(event.getId());

                log.debugf("Published outbox event: id=%s, type=%s, aggregateId=%s",
                        event.getId(), event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.errorf(e, "Failed to publish outbox event: id=%s", event.getId());
                // Stop processing this batch — remaining events will be retried next cycle
                break;
            }
        }

        if (!publishedIds.isEmpty()) {
            eventStore.markPublished(publishedIds);
            log.debugf("Marked %d outbox events as published", publishedIds.size());
        }
    }
}
