package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationRepository;
import com.wallet.shared.event.PaymentCompletedEvent;
import com.wallet.shared.money.Money;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

/**
 * Use case: handle payment event and dispatch notification.
 */
@Singleton
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationSender sender;

    @Inject
    public NotificationService(NotificationRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    /**
     * Process a PaymentCompletedEvent: create notification, dispatch, update status.
     */
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Notification notification = Notification.create(
                com.wallet.shared.util.IdGenerator.newId(),
                event.userId(),
                Notification.Type.EMAIL,
                "Payment " + event.status(),
                "Your payment of " + event.amount().amount().toPlainString() + " " + event.amount().currency()
                        + " (id=" + event.paymentId() + ") is now " + event.status() + ".",
                event.metadata().eventId());

        Notification saved = repository.save(notification);
        log.infof("Notification created: id=%s, userId=%s, eventId=%s", saved.id(), saved.userId(), event.metadata().eventId());

        try {
            sender.send(saved);
            Notification sent = saved.markSent();
            repository.save(sent);
            log.infof("Notification sent: id=%s", sent.id());
        } catch (RuntimeException ex) {
            Notification failed = saved.markFailed();
            repository.save(failed);
            log.errorf("Notification dispatch failed: id=%s, error=%s", saved.id(), ex.getMessage());
            throw ex;
        }
    }
}
