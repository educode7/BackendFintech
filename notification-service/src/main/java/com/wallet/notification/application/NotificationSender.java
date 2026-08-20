package com.wallet.notification.application;

import com.wallet.notification.domain.Notification;

public interface NotificationSender {
    void send(Notification notification);
}
