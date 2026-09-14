package com.wallet.payment.application;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.wallet.payment.domain.EventPublisher;
import com.wallet.payment.domain.IdempotencyStore;
import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;

/**
 * Use case: process a payment.
 * <p>
 * Orchestrates: idempotency check → create payment → persist → publish event.
 * Completely framework-free at the domain boundary — all infrastructure
 * is injected via ports (interfaces).
 */
@Singleton
public class ProcessPaymentUseCase {

    private static final Logger log = Logger.getLogger(ProcessPaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;
    private final EventPublisher eventPublisher;

    @ConfigProperty(name = "wallet.idempotency.ttl-hours", defaultValue = "24")
    int idempotencyTtlHours;

    @Inject
    public ProcessPaymentUseCase(PaymentRepository paymentRepository,
                                 IdempotencyStore idempotencyStore,
                                 EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Process a payment with idempotency guarantee.
     *
     * @param command the payment command
     * @param correlationId distributed tracing correlation ID
     * @return the payment response as JSON string
     */
    public Uni<PaymentResponse> execute(ProcessPaymentCommand command, String correlationId) {
        String key = command.idempotencyKey();

        // 1. Check idempotency slot
        IdempotencyStore.SlotState state = idempotencyStore.claim(key, idempotencyTtlHours);

        return switch (state) {
            case COMPLETED -> {
                // Replay cached response
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
                // We won the race — proceed to process
                yield processPayment(command, correlationId);
            }
            case DUPLICATE -> {
                // Another request is already processing this key
                log.warnf("Duplicate request detected for key=%s", key);
                yield Uni.createFrom().failure(
                        new com.wallet.payment.domain.exception.DuplicatePaymentException(key));
            }
            case FAILED -> {
                // Allow retry — re-claim
                log.infof("Retrying failed payment for key=%s", key);
                yield processPayment(command, correlationId);
            }
        };
    }

    private Uni<PaymentResponse> processPayment(ProcessPaymentCommand command, String correlationId) {
        // 2. Create domain entity
        String paymentId = com.wallet.shared.util.IdGenerator.newId();
        Payment payment = Payment.create(paymentId, command.userId(), command.amount(), command.idempotencyKey());

        // 3. Persist
        Payment saved = paymentRepository.save(payment);
        log.infof("Payment created: id=%s, userId=%s, amount=%s, key=%s",
                saved.id(), saved.userId(), saved.amount(), saved.idempotencyKey());

        // 4. Transition to PROCESSING then COMPLETED
        Payment processing = saved.startProcessing();
        Payment completed = processing.complete();
        Payment finalPayment = paymentRepository.save(completed);

        // 5. Publish event (async, fire-and-forget with error logging)
        eventPublisher.publishPaymentCompleted(
                finalPayment.id(),
                finalPayment.userId(),
                finalPayment.amount(),
                finalPayment.status().name(),
                correlationId);

        // 6. Cache response for idempotency replay
        PaymentResponse response = PaymentResponse.from(finalPayment);
        String responseJson = serializeResponse(response);
        idempotencyStore.complete(command.idempotencyKey(), responseJson, idempotencyTtlHours);

        return Uni.createFrom().item(response);
    }

    private PaymentResponse deserializeResponse(String json) {
        return com.wallet.shared.util.JsonUtil.fromJson(json, PaymentResponse.class);
    }

    private String serializeResponse(PaymentResponse response) {
        return com.wallet.shared.util.JsonUtil.toJson(response);
    }
}
