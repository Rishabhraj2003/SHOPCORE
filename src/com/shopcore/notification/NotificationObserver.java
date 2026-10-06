package com.shopcore.notification;

/** Observer pattern: anything that wants to hear about notifications implements this. */
public interface NotificationObserver {
    void onNotification(String userId, String message);
}
