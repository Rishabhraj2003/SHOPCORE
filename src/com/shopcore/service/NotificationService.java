package com.shopcore.service;

import com.shopcore.logging.AppLogger;
import com.shopcore.notification.NotificationObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The "subject" of the Observer pattern: services call notifyUser, every registered observer reacts. */
public class NotificationService {
    private static final Logger LOG = AppLogger.get(NotificationService.class);
    private final List<NotificationObserver> observers = new CopyOnWriteArrayList<>();

    public void register(NotificationObserver observer) { observers.add(observer); }

    public void notifyUser(String userId, String message) {
        for (NotificationObserver observer : observers) {
            try {
                observer.onNotification(userId, message);
            } catch (RuntimeException e) {          // one broken observer must not break an order
                LOG.log(Level.WARNING, "Notification observer failed", e);
            }
        }
    }
}
