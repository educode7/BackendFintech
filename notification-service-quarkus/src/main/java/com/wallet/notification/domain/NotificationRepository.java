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
}
