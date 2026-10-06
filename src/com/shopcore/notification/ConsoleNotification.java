package com.shopcore.notification;

/** Prints the notification on screen, like a push notification. */
public class ConsoleNotification implements NotificationObserver {
    @Override
    public void onNotification(String userId, String message) {
        System.out.println("[NOTIFICATION to " + userId + "] " + message);
    }
}
