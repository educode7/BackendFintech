package com.wallet.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Notification aggregate.
 *
 * Why we persist the notification even though it gets "sent" by a logger:
 * - Auditable trail: ops can later see "what notification did user X receive?".
 * - Idempotency: the UNIQUE constraint on {@code processedEventId} is the
 *   last line of defence if Redis loses the idempotency marker.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "processed_event_id", nullable = false, length = 64, unique = true)
    private String processedEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
