package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sender that just logs. Real implementations (SES, SendGrid, FCM) plug in
 * here later — the contract is {@link NotificationSender}.
 */
@Component
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        if (notification.getType() == NotificationType.EMAIL) {
            log.info("[EMAIL] to={} subject={} body={}",
                notification.getUserId(), notification.getSubject(), notification.getBody());
        } else {
            log.info("[PUSH] to={} title={} body={}",
                notification.getUserId(), notification.getSubject(), notification.getBody());
        }
    }
}
