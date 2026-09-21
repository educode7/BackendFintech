package com.wallet.payment.application;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.UserTransaction;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.wallet.payment.domain.IdempotencyStore;
import com.wallet.payment.domain.OutboxEvent;
import com.wallet.payment.domain.OutboxRepository;
import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.shared.money.Money;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Use case: process a payment.
 * <p>
 * Orchestrates: idempotency check → create payment → persist + outbox in same TX.
 * The outbox event is published to Kafka by a separate polling publisher,
 * guaranteeing at-least-once delivery even if Kafka is temporarily down.
 */
@Singleton
public class ProcessPaymentUseCase {

    private static final Logger log = Logger.getLogger(ProcessPaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;
    private final OutboxRepository outboxRepository;
    private final UserTransaction userTransaction;

    @ConfigProperty(name = "wallet.idempotency.ttl-hours", defaultValue = "24")
    int idempotencyTtlHours;

    @Inject
    public ProcessPaymentUseCase(PaymentRepository paymentRepository,
                                 IdempotencyStore idempotencyStore,
                                 OutboxRepository outboxRepository,
                                 UserTransaction userTransaction) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
        this.outboxRepository = outboxRepository;
        this.userTransaction = userTransaction;
    }

    /**
     * Claim an idempotency slot for a payment.
     * Called directly by the resource for synchronous flow.
     */
    public IdempotencyStore.SlotState claimIdempotency(String key) {
        return idempotencyStore.claim(key, idempotencyTtlHours);
    }

    /**
     * Get cached response for idempotent replay.
     * Called directly by the resource for synchronous flow.
     */
    public Optional<String> getCachedResponse(String key) {
        return idempotencyStore.getCachedResponse(key);
    }

    /**
     * Process payment with idempotency guarantee.
     *
     * @param command the payment command
     * @param correlationId distributed tracing correlation ID
     * @return the payment response as JSON string
     */
    public Uni<PaymentResponse> execute(ProcessPaymentCommand command, String correlationId) {
        String key = command.idempotencyKey();

        // 1. Check idempotency slot — run on worker thread (Redis is blocking)
        return Uni.createFrom().item(() -> idempotencyStore.claim(key, idempotencyTtlHours))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onItem().transformToUni(state -> {
                    return switch (state) {
                        case COMPLETED -> {
                            log.debugf("Idempotent replay for key=%s", key);
                            Optional<String> cached = idempotencyStore.getCachedResponse(key);
                            if (cached.isPresent()) {
                                yield Uni.createFrom().item(deserializeResponse(cached.get()));
                            } else {
                                yield Uni.createFrom().failure(
                                        new IllegalStateException("Completed slot but no cached response for key=" + key));
                            }
                        }
                        case IN_PROGRESS -> {
                            log.infof("Processing payment for key=%s", key);
                            yield Uni.createFrom().item(() -> {
                                try {
                                    return processPaymentSync(command, correlationId);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
                        }
                        case DUPLICATE -> {
                            log.warnf("Duplicate request detected for key=%s", key);
                            yield Uni.createFrom().failure(
                                    new com.wallet.payment.domain.exception.DuplicatePaymentException(key));
                        }
                        case FAILED -> {
                            log.infof("Retrying failed payment for key=%s", key);
                            yield Uni.createFrom().item(() -> {
                                try {
                                    return processPaymentSync(command, correlationId);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
                        }
                    };
                });
    }

    /**
     * Process payment and write to outbox in the SAME transaction.
     * If either payment save or outbox save fails, both are rolled back.
     * The outbox poller will publish the event to Kafka asynchronously.
     * Uses UserTransaction for explicit transaction management.
     *
     * IMPORTANT: The domain Payment is immutable — each state transition (startProcessing,
     * complete) returns a NEW instance with an incremented version. But the JPA @Version
     * field is for optimistic locking, not domain versioning. We create the payment
     * directly in COMPLETED state to avoid the version mismatch that occurs when calling
     * save() twice (merge with version=2 vs DB version=0 → StaleObjectStateException).
     */
    @WithSpan("process-payment")
    public PaymentResponse processPaymentSync(
            @SpanAttribute("payment.idempotency_key") ProcessPaymentCommand command,
            @SpanAttribute("correlation.id") String correlationId) throws Exception {
        userTransaction.begin();
        try {
            // 2. Create domain entity — skip PENDING/PROCESSING, go directly to COMPLETED
            //    This avoids double-save version mismatch with @Version optimistic locking
            String paymentId = com.wallet.shared.util.IdGenerator.newId();
            java.time.Instant now = java.time.Instant.now();
            Payment payment = Payment.of(paymentId, command.accountId(), command.userId(),
                    command.amount(), command.idempotencyKey(),
                    Payment.Status.COMPLETED, 0, now, now,
                    command.paymentType(),
                    command.beneficiaryName(), command.beneficiaryDocumentType(), command.beneficiaryDocumentNumber(),
                    command.beneficiaryAccountNumber(), command.beneficiaryBankCode(), command.beneficiaryBankName(),
                    command.senderName(), command.senderDocumentType(), command.senderDocumentNumber(),
                    command.reference(), command.externalReference(),
                    command.channel(), command.ipAddress(), command.userAgent(),
                    now, null, null, 0,
                    command.feeAmount());
            log.infof("Payment created: id=%s, userId=%s, amount=%s, key=%s",
                    payment.id(), payment.userId(), payment.amount(), payment.idempotencyKey());

            // 3. Persist payment (single save — no version conflict)
            Payment saved = paymentRepository.save(payment);

            // 4. Write to outbox (same transaction as payment)
            String eventId = com.wallet.shared.util.IdGenerator.newId();
            String eventType = "PaymentCompleted";
            String payload = buildEventPayload(saved, correlationId, eventId);

            OutboxEvent outboxEvent = OutboxEvent.create(
                    eventType,
                    saved.id(),
                    "Payment",
                    payload,
                    correlationId
            );
            outboxRepository.save(outboxEvent);
            log.infof("Outbox event written: type=%s, paymentId=%s", eventType, saved.id());

            userTransaction.commit();

            // 5. Cache response for idempotency replay (after commit)
            PaymentResponse response = PaymentResponse.from(saved);
            String responseJson = serializeResponse(response);
            idempotencyStore.complete(command.idempotencyKey(), responseJson, idempotencyTtlHours);

            return response;
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }
    }

    private String buildEventPayload(Payment payment, String correlationId, String eventId) {
        return new io.vertx.core.json.JsonObject()
                .put("eventId", eventId)
                .put("eventType", "PaymentCompleted")
                .put("aggregateId", payment.id())
                .put("aggregateType", "Payment")
                .put("correlationId", correlationId)
                .put("payload", new io.vertx.core.json.JsonObject()
                        .put("paymentId", payment.id())
                        .put("accountId", payment.accountId())
                        .put("userId", payment.userId())
                        .put("amount", new io.vertx.core.json.JsonObject()
                                .put("amount", payment.amount().amount().toPlainString())
                                .put("currency", payment.amount().currency()))
                        .put("status", payment.status().name())
                        .put("paymentType", payment.paymentType())
                        .put("beneficiaryName", payment.beneficiaryName())
                        .put("beneficiaryAccountNumber", payment.beneficiaryAccountNumber())
                        .put("reference", payment.reference())
                        .put("channel", payment.channel()))
                .encode();
    }

    private PaymentResponse deserializeResponse(String json) {
        return com.wallet.shared.util.JsonUtil.fromJson(json, PaymentResponse.class);
    }

    private String serializeResponse(PaymentResponse response) {
        return com.wallet.shared.util.JsonUtil.toJson(response);
    }
}
