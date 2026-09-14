package com.wallet.payment.infrastructure.kafka;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.payment.domain.OutboxEvent;
import com.wallet.payment.domain.OutboxRepository;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

/**
 * Polls the transactional outbox and publishes events to Kafka.
 * <p>
 * This is the ONLY component that publishes to Kafka. The use case
 * writes to the outbox table in the same transaction as business data,
 * and this poller picks up unpublished events and sends them.
 * <p>
 * Guarantees: at-least-once delivery. If publishing fails, the event
 * remains unpublished and will be retried on the next poll cycle.
 */
@ApplicationScoped
public class OutboxPoller {

    private static final Logger log = Logger.getLogger(OutboxPoller.class);
    private static final String TOPIC = "payment.events";
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;

    @Channel("payment-events-out")
    Emitter<String> emitter;

    @Inject
    public OutboxPoller(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Poll unpublished events every 500ms and publish to Kafka.
     */
    @Scheduled(every = "500ms")
    void poll() {
        List<OutboxEvent> events = outboxRepository.findUnpublished(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        log.debugf("Polling outbox: found %d unpublished events", events.size());

        List<String> publishedIds = new java.util.ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                        .withTopic(TOPIC)
                        .withKey(event.aggregateId())
                        .build();

                emitter.send(Message.of(event.payload()).addMetadata(metadata));
                publishedIds.add(event.id());

                log.debugf("Published outbox event: id=%s, type=%s, aggregateId=%s",
                        event.id(), event.eventType(), event.aggregateId());
            } catch (Exception e) {
                log.errorf(e, "Failed to publish outbox event: id=%s", event.id());
                // Stop processing this batch — remaining events will be retried next cycle
                break;
            }
        }

        if (!publishedIds.isEmpty()) {
            outboxRepository.markPublished(publishedIds);
            log.debugf("Marked %d outbox events as published", publishedIds.size());
        }
    }
}
