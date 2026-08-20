package com.wallet.account.infrastructure.kafka;

import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.kafka.KafkaTopics;
import com.wallet.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Publishes account events to {@code account.events}.
 *
 * Headers attached: {@code X-Correlation-Id}, {@code event-type}, {@code event-id}
 * so consumers (notification-service today, more tomorrow) can route without
 * parsing the body.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(AccountEvent event) {
        String payload = JsonUtil.toJson(event);
        Message<String> message = MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, KafkaTopics.ACCOUNT_EVENTS)
            .setHeader(KafkaHeaders.KEY, event.accountId())
            .setHeader(KafkaTopics.HEADER_EVENT_TYPE, event.getClass().getSimpleName())
            .setHeader(KafkaTopics.HEADER_EVENT_ID, event.metadata().eventId())
            .setHeader(KafkaTopics.HEADER_CORRELATION_ID, event.metadata().correlationId())
            .build();
        kafkaTemplate.send(message);
        log.info("account event published type={} accountId={} correlationId={}",
            event.getClass().getSimpleName(), event.accountId(), event.metadata().correlationId());
    }
}
