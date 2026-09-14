package com.wallet.notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Notification Domain - Extended")
class NotificationExtendedTest {

    @Nested
    @DisplayName("Type enum")
    class TypeEnum {

        @Test
        @DisplayName("EMAIL and PUSH should exist")
        void values() {
            assertEquals(2, Notification.Type.values().length);
            assertNotNull(Notification.Type.EMAIL);
            assertNotNull(Notification.Type.PUSH);
        }
    }

    @Nested
    @DisplayName("Status enum")
    class StatusEnum {

        @Test
        @DisplayName("PENDING, SENT, FAILED should exist")
        void values() {
            assertEquals(3, Notification.Status.values().length);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("markSent should return new instance")
        void markSentReturnsNew() {
            Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                    "Subject", "Body", "evt-1");
            Notification sent = n.markSent();

            assertNotSame(n, sent);
            assertEquals(Notification.Status.PENDING, n.status());
            assertEquals(Notification.Status.SENT, sent.status());
        }

        @Test
        @DisplayName("markFailed should return new instance")
        void markFailedReturnsNew() {
            Notification n = Notification.create("n-1", "user-1", Notification.Type.PUSH,
                    "Subject", "Body", "evt-1");
            Notification failed = n.markFailed();

            assertNotSame(n, failed);
            assertEquals(Notification.Status.PENDING, n.status());
            assertEquals(Notification.Status.FAILED, failed.status());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty subject and body")
        void emptyStrings() {
            Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                    "", "", "evt-1");
            assertEquals("", n.subject());
            assertEquals("", n.body());
        }

        @Test
        @DisplayName("should preserve processedEventId")
        void preservesEventId() {
            Notification n = Notification.create("n-1", "user-1", Notification.Type.EMAIL,
                    "Sub", "Body", "evt-123");
            assertEquals("evt-123", n.processedEventId());
        }
    }
}
