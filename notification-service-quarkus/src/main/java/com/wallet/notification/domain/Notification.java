package com.wallet.notification.domain;

/**
 * Notification aggregate root.
 * Pure domain object — no framework dependencies.
 */
public final class Notification {

    public enum Type { EMAIL, PUSH }
    public enum Status { PENDING, SENT, FAILED }

    private final String id;
    private final String userId;
    private final Type type;
    private final String subject;
    private final String body;
    private final Status status;
    private final String processedEventId;
    private final java.time.Instant createdAt;
    private final java.time.Instant sentAt;
    private final java.time.Instant readAt;

    private Notification(String id, String userId, Type type, String subject, String body,
                         Status status, String processedEventId, java.time.Instant createdAt,
                         java.time.Instant sentAt, java.time.Instant readAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.processedEventId = processedEventId;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public static Notification create(String id, String userId, Type type, String subject,
                                      String body, String processedEventId) {
        return new Notification(id, userId, type, subject, body, Status.PENDING,
                processedEventId, java.time.Instant.now(), null, null);
    }

    /**
     * Factory: reconstitute from persistence preserving all persisted timestamps.
     */
    public static Notification of(String id, String userId, Type type, String subject, String body,
                                  Status status, String processedEventId,
                                  java.time.Instant createdAt, java.time.Instant sentAt,
                                  java.time.Instant readAt) {
        return new Notification(id, userId, type, subject, body, status,
                processedEventId, createdAt, sentAt, readAt);
    }

    public Notification markSent() {
        return new Notification(id, userId, type, subject, body, Status.SENT,
                processedEventId, createdAt, java.time.Instant.now(), readAt);
    }

    public Notification markFailed() {
        return new Notification(id, userId, type, subject, body, Status.FAILED,
                processedEventId, createdAt, null, readAt);
    }

    /**
     * Mark this notification as read by the user. Idempotent: if already read,
     * returns this instance unchanged.
     */
    public Notification markAsRead() {
        if (readAt != null) {
            return this;
        }
        return new Notification(id, userId, type, subject, body, status,
                processedEventId, createdAt, sentAt, java.time.Instant.now());
    }

    public String id() { return id; }
    public String userId() { return userId; }
    public Type type() { return type; }
    public String subject() { return subject; }
    public String body() { return body; }
    public Status status() { return status; }
    public String processedEventId() { return processedEventId; }
    public java.time.Instant createdAt() { return createdAt; }
    public java.time.Instant sentAt() { return sentAt; }
    public java.time.Instant readAt() { return readAt; }
}
