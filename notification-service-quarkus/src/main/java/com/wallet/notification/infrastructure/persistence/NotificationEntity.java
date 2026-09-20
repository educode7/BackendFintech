package com.wallet.notification.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for notifications.
 */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "processed_event_id", nullable = false, length = 64, unique = true)
    private String processedEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public NotificationEntity() {}

    public static NotificationEntity fromDomain(com.wallet.notification.domain.Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.id = n.id();
        e.userId = n.userId();
        e.type = n.type().name();
        e.subject = n.subject();
        e.body = n.body();
        e.status = n.status().name();
        e.processedEventId = n.processedEventId();
        e.createdAt = n.createdAt();
        e.sentAt = n.sentAt();
        return e;
    }

    public com.wallet.notification.domain.Notification toDomain() {
        com.wallet.notification.domain.Notification n = com.wallet.notification.domain.Notification.create(
                id, userId, com.wallet.notification.domain.Notification.Type.valueOf(type),
                subject, body, processedEventId);
        if (com.wallet.notification.domain.Notification.Status.valueOf(status) == com.wallet.notification.domain.Notification.Status.SENT) {
            return n.markSent();
        } else if (com.wallet.notification.domain.Notification.Status.valueOf(status) == com.wallet.notification.domain.Notification.Status.FAILED) {
            return n.markFailed();
        }
        return n;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getStatus() { return status; }
    public String getProcessedEventId() { return processedEventId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }

    // Setters for updates
    public void setStatus(String status) { this.status = status; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBody(String body) { this.body = body; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
