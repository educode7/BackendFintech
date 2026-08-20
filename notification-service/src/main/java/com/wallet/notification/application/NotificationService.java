package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;
import com.wallet.notification.infrastructure.persistence.NotificationJpaRepository;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use-case orchestration: turn a domain event into a persisted + dispatched notification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationJpaRepository repository;
    private final NotificationSender sender;

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Notification notification = new Notification();
        notification.setId(IdGenerator.newId());
        notification.setUserId(event.userId());
        notification.setType(NotificationType.EMAIL);
        notification.setSubject("Payment " + event.status());
        notification.setBody("Your payment of "
            + event.amount().amount().toPlainString() + " " + event.amount().currency()
            + " (id=" + event.paymentId() + ") is now " + event.status() + ".");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setProcessedEventId(event.metadata().eventId());
        notification.setCreatedAt(Instant.now());

        Notification saved = repository.save(notification);
        try {
            sender.send(saved);
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(Instant.now());
            repository.save(saved);
            log.info("notification sent userId={} eventId={}", event.userId(), event.metadata().eventId());
        } catch (RuntimeException ex) {
            saved.setStatus(NotificationStatus.FAILED);
            repository.save(saved);
            log.error("notification dispatch failed userId={} eventId={}", event.userId(), event.metadata().eventId(), ex);
            throw ex;
        }
    }
}
