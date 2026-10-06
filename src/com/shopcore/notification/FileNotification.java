package com.shopcore.notification;

import com.shopcore.config.AppConfig;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.util.DateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

/** Keeps a permanent copy of every notification (acts like a sent-email log). */
public class FileNotification implements NotificationObserver {
    @Override
    public synchronized void onNotification(String userId, String message) {
        String line = DateUtil.format(LocalDateTime.now()) + " | " + userId + " | " + message;
        try {
            Files.createDirectories(AppConfig.dataDir());
            Files.write(AppConfig.dataDir().resolve("notifications.log"), List.of(line),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new ShopCoreException("Could not store notification", e);
        }
    }
}
