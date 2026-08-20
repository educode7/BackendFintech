package com.wallet.payment.application;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.infrastructure.idempotency.IdempotencyService;
import com.wallet.payment.infrastructure.idempotency.IdempotencyService.IdempotencyResult;
import com.wallet.payment.infrastructure.kafka.PaymentEventPublisher;
import com.wallet.payment.infrastructure.persistence.PaymentJpaRepository;
import com.wallet.shared.context.CorrelationContext;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Orchestrates the payment use case:
 *  1. Run inside an idempotency wrapper (Redis SET NX EX).
 *  2. Persist the payment row (unique idempotency_key constraint is the second line of defence).
 *  3. Publish PaymentCompletedEvent to Kafka AFTER the JPA transaction commits.
 *
 * Why publish after commit (and not inside the transaction):
 * - If the DB rolls back, we don't want consumers to act on a phantom event.
 * - Kafka publish in the same transaction is theoretically cleaner (transactional outbox)
 *   but adds operational cost we explicitly defer as a TODO.
 *
 * Trade-off documented in README: this leaves a window where DB committed but Kafka
 * publish failed — mitigated by the consumer being idempotent on eventId.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final PaymentJpaRepository repository;
    private final IdempotencyService idempotency;
    private final PaymentEventPublisher publisher;

    public PaymentResponse process(ProcessPaymentCommand cmd) {
        IdempotencyResult<PaymentResponse> result = idempotency.executeOnce(
            cmd.idempotencyKey(),
            PaymentResponse.class,
            () -> doProcess(cmd)
        );
        if (result.replayed()) {
            log.info("payment idempotent replay key={} paymentId={}",
                cmd.idempotencyKey(), result.value().id());
        }
        return result.value();
    }

    @Transactional
    protected PaymentResponse doProcess(ProcessPaymentCommand cmd) {
        Payment payment = new Payment();
        payment.setId(com.wallet.shared.util.IdGenerator.newId());
        payment.setUserId(cmd.userId());
        payment.setAmount(cmd.amount().amount());
        payment.setCurrency(cmd.amount().currency());
        payment.setStatus(com.wallet.payment.domain.PaymentStatus.COMPLETED);
        payment.setIdempotencyKey(cmd.idempotencyKey());

        Payment saved = repository.save(payment);
        log.info("payment persisted id={} userId={} amount={} {}",
            saved.getId(), saved.getUserId(), saved.getAmount(), saved.getCurrency());

        registerAfterCommitPublish(saved);
        return PaymentResponse.from(saved);
    }

    /**
     * Hook the publish to the JPA transaction's commit lifecycle.
     * TODO(production): replace with transactional outbox pattern — see README.
     */
    private void registerAfterCommitPublish(Payment saved) {
        String correlationId = CorrelationContext.currentOrNull();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            saved.getId(),
            saved.getUserId(),
            saved.money(),
            saved.getStatus().name(),
            new EventMetadata(
                com.wallet.shared.util.IdGenerator.newId(),
                Instant.now(),
                correlationId,
                1
            )
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publish(event);
            }
        });
    }
}
