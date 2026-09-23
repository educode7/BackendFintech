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

    @Test
    @DisplayName("markAsRead should set readAt and preserve timestamps")
    void markAsReadSetsReadAt() {
        Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                "Test", "Body", "evt-1");
        Notification read = n.markAsRead();

        assertNull(n.readAt());
        assertNotNull(read.readAt());
        assertEquals(n.createdAt(), read.createdAt());
        assertEquals(Notification.Status.PENDING, read.status());
        assertEquals("Test", read.subject());
    }

    @Test
    @DisplayName("markAsRead should be idempotent")
    void markAsReadIdempotent() {
        Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                "Test", "Body", "evt-1");
        Notification first = n.markAsRead();
        Notification second = first.markAsRead();

        assertSame(first, second);
        assertEquals(first.readAt(), second.readAt());
    }

    @Test
    @DisplayName("of should preserve createdAt/sentAt/readAt from persistence")
    void ofPreservesTimestamps() {
        java.time.Instant createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z");
        java.time.Instant sentAt = java.time.Instant.parse("2026-01-01T00:00:01Z");
        java.time.Instant readAt = java.time.Instant.parse("2026-01-01T00:00:02Z");

        Notification n = Notification.of("n-1", "user-1", Notification.Type.EMAIL,
                "Subject", "Body", Notification.Status.SENT, "evt-1",
                createdAt, sentAt, readAt);

        assertEquals(createdAt, n.createdAt());
        assertEquals(sentAt, n.sentAt());
        assertEquals(readAt, n.readAt());
        assertEquals(Notification.Status.SENT, n.status());
    }
}
