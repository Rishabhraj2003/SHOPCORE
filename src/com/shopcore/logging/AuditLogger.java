package com.shopcore.logging;

import com.shopcore.config.AppConfig;
import com.shopcore.enums.AuditAction;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.util.DateUtil;
import com.shopcore.util.MoneyUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Business audit trail: who did what, when, with what result. One line per event in data/audit.log. */
public final class AuditLogger {
    private static final AuditLogger INSTANCE = new AuditLogger();
    private static final Logger LOG = AppLogger.get(AuditLogger.class);

    private final Object lock = new Object();

    private AuditLogger() {}

    public static AuditLogger getInstance() { return INSTANCE; }

    private Path file() { return AppConfig.dataDir().resolve("audit.log"); }

    public void log(String userId, AuditAction action, String ref) {
        log(userId, action, ref, null, "SUCCESS");
    }

    public void log(String userId, AuditAction action, String ref, Double amount, String status) {
        String line = String.format("%s | USER: %s | ACTION: %s | REF: %s | AMOUNT: %s | STATUS: %s",
                DateUtil.format(LocalDateTime.now()),
                userId == null ? "-" : userId,
                action,
                ref == null ? "-" : ref,
                amount == null ? "-" : MoneyUtil.format(amount),
                status);
        synchronized (lock) {                       // many threads may log at the same time
            try {
                Files.createDirectories(file().getParent());
                Files.write(file(), List.of(line), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not write audit entry", e);
            }
        }
    }

    /** Last n lines; if userId is given only that user's events are returned. */
    public List<String> readLast(int n, String userId) {
        synchronized (lock) {
            if (!Files.exists(file())) return List.of();
            try {
                List<String> lines = Files.readAllLines(file(), StandardCharsets.UTF_8);
                if (userId != null && !userId.isBlank()) {
                    String marker = "USER: " + userId.trim() + " |";
                    lines = lines.stream().filter(l -> l.contains(marker)).collect(Collectors.toList());
                }
                return lines.subList(Math.max(0, lines.size() - n), lines.size());
            } catch (IOException e) {
                throw new ShopCoreException("Could not read the audit log", e);
            }
        }
    }
}
