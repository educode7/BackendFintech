package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingNotificationSenderTest {

    @Test
    @DisplayName("Envía EMAIL logueando el subject y body")
    void send_email_loguea() {
        LoggingNotificationSender sender = new LoggingNotificationSender();
        Notification n = new Notification();
        n.setId("n-1");
        n.setUserId("user-1");
        n.setType(NotificationType.EMAIL);
        n.setSubject("Test subject");
        n.setBody("Test body");
        n.setStatus(NotificationStatus.PENDING);
        n.setProcessedEventId("evt-1");
        n.setCreatedAt(Instant.now());

        sender.send(n);
    }

    @Test
    @DisplayName("Envía PUSH logueando el subject y body")
    void send_push_loguea() {
        LoggingNotificationSender sender = new LoggingNotificationSender();
        Notification n = new Notification();
        n.setId("n-2");
        n.setUserId("user-2");
        n.setType(NotificationType.PUSH);
        n.setSubject("Push title");
        n.setBody("Push body");
        n.setStatus(NotificationStatus.PENDING);
        n.setProcessedEventId("evt-2");
        n.setCreatedAt(Instant.now());

        sender.send(n);
    }

    @Test
    @DisplayName("El sender implementa NotificationSender (composición sobre herencia)")
    void implementsInterface() {
        LoggingNotificationSender sender = new LoggingNotificationSender();
        assertThat(sender).isInstanceOf(NotificationSender.class);
    }
}
