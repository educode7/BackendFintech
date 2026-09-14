package com.wallet.notification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.application.NotificationSender;
import com.wallet.notification.application.LoggingNotificationSender;

@DisplayName("Notification Domain")
class NotificationTest {

    @Test
    @DisplayName("should create notification in PENDING state")
    void shouldCreate() {
        Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                "Test", "Body", "evt-1");

        assertEquals("n-1", n.id());
        assertEquals("user-1", n.userId());
        assertEquals(Notification.Type.EMAIL, n.type());
        assertEquals(Notification.Status.PENDING, n.status());
        assertNotNull(n.createdAt());
        assertNull(n.sentAt());
    }

    @Test
    @DisplayName("should transition to SENT")
    void shouldTransitionToSent() {
        Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                "Test", "Body", "evt-1");
        Notification sent = n.markSent();

        assertEquals(Notification.Status.SENT, sent.status());
        assertNotNull(sent.sentAt());
    }

    @Test
    @DisplayName("should transition to FAILED")
    void shouldTransitionToFailed() {
        Notification n = Notification.create("n-1", "user-1", Notification.Type.PUSH,
                "Test", "Body", "evt-1");
        Notification failed = n.markFailed();

        assertEquals(Notification.Status.FAILED, failed.status());
        assertNull(failed.sentAt());
    }

    @Test
    @DisplayName("LoggingNotificationSender implements NotificationSender")
    void senderImplementsInterface() {
        LoggingNotificationSender sender = new LoggingNotificationSender();
        assertInstanceOf(NotificationSender.class, sender);
    }
}
