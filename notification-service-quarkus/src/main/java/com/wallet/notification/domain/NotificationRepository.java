package com.wallet.notification.domain;

import java.util.Optional;

/**
 * Port: notification persistence.
 */
public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(String id);
    java.util.List<Notification> findByUserId(String userId, int offset, int limit);
    long countByUserId(String userId);

    /**
     * Mark a single notification as read. Idempotent.
     * Returns the updated notification, or empty if not found.
     */
    Optional<Notification> markAsRead(String id);

    /**
     * Mark all unread notifications for a user as read.
     *
     * @return number of rows updated (previously unread only)
     */
    long markAllAsRead(String userId);
}
