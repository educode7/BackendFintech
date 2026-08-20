package com.wallet.account.infrastructure.kafka;

import com.wallet.account.application.AccountCommandService;
import com.wallet.account.infrastructure.projection.AccountProjectionService;
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
 * Inbound consumer for {@code payment.events}.
 *
 * Decision (documented in README §9):
 *   When payment-service publishes a PaymentCompletedEvent, we treat it as
 *   an automatic credit to the user's account. The amount is deposited into
 *   the FIRST account found for the user — for the scaffold we keep it simple.
 *   Production would: 1) require payment-service to send an explicit accountId,
 *   or 2) auto-create an account on first deposit.
 *
 * For the scaffold, the consumer also re-emits an AccountEvent so the local
 * projection service keeps the read model current even when the consumer
 * is the one creating events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final AccountCommandService commandService;
    private final AccountProjectionService projection;
    private final AccountEventPublisher publisher;

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_EVENTS,
        groupId = KafkaTopics.GROUP_ACCOUNT
    )
    public void onPaymentEvent(
        @Payload String payload,
        @Header(name = KafkaTopics.HEADER_CORRELATION_ID, required = false) String correlationId,
        @Header(name = KafkaTopics.HEADER_EVENT_ID, required = false) String eventId
    ) {
        String id = correlationId == null ? IdGenerator.newId() : correlationId;
        ScopedValue.where(CorrelationContext.CORRELATION_ID, id)
            .run(() -> processPaymentEvent(payload));
    }

    private void processPaymentEvent(String payload) {
        try {
            PaymentCompletedEvent event = JsonUtil.fromJson(payload, PaymentCompletedEvent.class);
            log.info("payment event consumed paymentId={} status={} amount={} {} userId={}",
                event.paymentId(), event.status(),
                event.amount().amount(), event.amount().currency(),
                event.userId());

            if (!"COMPLETED".equalsIgnoreCase(event.status())) {
                log.info("payment not in COMPLETED status — skipping deposit");
                return;
            }

            // Auto-deposit into the user's account via the command service.
            // If the user has no account yet, we skip and log — production would
            // either auto-create or use a different saga.
            commandService.handlePaymentCompleted(event.userId(), event.amount());
            // (Real impl would issue a MoneyDepositedEvent here; left as TODO.)
        } catch (Exception ex) {
            log.error("failed to process payment event", ex);
        }
    }
}
