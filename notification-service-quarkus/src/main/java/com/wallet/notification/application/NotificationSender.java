package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;

/**
 * Port: notification dispatch.
 */
public interface NotificationSender {
    void send(Notification notification);
}
