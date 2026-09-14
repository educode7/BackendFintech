package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * Logging notification sender — placeholder for real implementations.
 */
@ApplicationScoped
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = Logger.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(Notification notification) {
        if (notification.type() == Notification.Type.EMAIL) {
            log.infof("[EMAIL] to=%s subject=%s body=%s",
                    notification.userId(), notification.subject(), notification.body());
        } else {
            log.infof("[PUSH] to=%s title=%s body=%s",
                    notification.userId(), notification.subject(), notification.body());
        }
    }
}
