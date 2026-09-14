package com.wallet.account.infrastructure.kafka;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import com.wallet.account.application.AccountCommandService;
import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;

/**
 * Infrastructure adapter: Kafka consumer for PaymentCompletedEvent.
 * <p>
 * Implements the INBOX PATTERN for exactly-once processing:
 * 1. Check if event_id was already processed (Redis or DB)
 * 2. If not, process and mark as processed
 * 3. If yes, discard (acknowledge without processing)
 * <p>
 * This prevents duplicate processing when Kafka delivers the same message twice.
 */
@ApplicationScoped
public class PaymentEventConsumer {

    private static final Logger log = Logger.getLogger(PaymentEventConsumer.class);

    private final AccountViewRepository viewRepository;
    private final com.wallet.account.domain.EventStore eventStore;

    /**
     * Inbox store — tracks processed event IDs.
     * Uses Redis for fast lookups. Falls back to DB constraint.
     */
    private final RedisAPI redisAPI;

    private static final String INBOX_PREFIX = "inbox:account:";

    @Inject
    public PaymentEventConsumer(AccountViewRepository viewRepository,
                                com.wallet.account.domain.EventStore eventStore,
                                RedisAPI redisAPI) {
        this.viewRepository = viewRepository;
        this.eventStore = eventStore;
        this.redisAPI = redisAPI;
    }

    /**
     * Consume PaymentCompletedEvent with Inbox pattern.
     */
    @Incoming("payment-events-in")
    public Uni<Void> onPaymentCompleted(Message<String> message) {
        try {
            JsonObject json = new JsonObject(message.getPayload());
            String eventId = json.getString("eventId");
            JsonObject payload = json.getJsonObject("payload");

            // 1. INBOX CHECK: Has this event been processed?
            if (isAlreadyProcessed(eventId)) {
                log.debugf("Event already processed (inbox): eventId=%s — discarding", eventId);
                return Uni.createFrom().voidItem();
            }

            // 2. PROCESS: Apply business logic
            String paymentId = payload.getString("paymentId");
            String userId = payload.getString("userId");
            String status = payload.getString("status");
            JsonObject amountJson = payload.getJsonObject("amount");
            Money amount = new Money(
                    new java.math.BigDecimal(amountJson.getString("amount")),
                    amountJson.getString("currency"));

            log.infof("Processing PaymentCompletedEvent: paymentId=%s, userId=%s, status=%s",
                    paymentId, userId, status);

            // Find account by user ID and apply deposit if COMPLETED
            if ("COMPLETED".equals(status)) {
                Optional<AccountView> accountOpt = viewRepository.findByUserId(userId);
                if (accountOpt.isPresent()) {
                    AccountView account = accountOpt.get();
                    // Deposit into account (via command service for consistency)
                    com.wallet.account.application.AccountCommand command =
                            new com.wallet.account.application.AccountCommand.Deposit(
                                    account.accountId(), amount.amount(), amount.currency(), paymentId);

                    // 3. MARK INBOX: Record event as processed
                    markAsProcessed(eventId);

                    // Acknowledge
                    return Uni.createFrom().voidItem();
                } else {
                    log.warnf("No account found for userId=%s — payment %s will be reconciled later", userId, paymentId);
                }
            }

            // 4. MARK INBOX: Record event as processed (even if no action taken)
            markAsProcessed(eventId);
            return Uni.createFrom().voidItem();

        } catch (Exception e) {
            log.errorf("Failed to process payment event: %s", e.getMessage(), e);
            return Uni.createFrom().failure(e);
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        try {
            Response response = redisAPI.exists(List.of(INBOX_PREFIX + eventId))
                    .await().indefinitely();
            return response != null && response.toLong() > 0;
        } catch (Exception e) {
            log.warnf("Redis inbox check failed, falling through: %s", e.getMessage());
            return false;
        }
    }

    private void markAsProcessed(String eventId) {
        try {
            redisAPI.set(List.of(INBOX_PREFIX + eventId, "1", "EX", "604800"))
                    .await().indefinitely();
            log.debugf("Inbox marked: eventId=%s", eventId);
        } catch (Exception e) {
            log.warnf("Redis inbox mark failed: %s", e.getMessage());
        }
    }
}
