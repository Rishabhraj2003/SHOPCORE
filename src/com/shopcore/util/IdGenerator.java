package com.shopcore.util;

import com.shopcore.config.AppConfig;
import com.shopcore.exception.ShopCoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Produces readable, restart-safe ids such as U1001, P1002, ORD1003 (counters live in a file). */
public final class IdGenerator {
    private static final Object LOCK = new Object();

    private IdGenerator() {}

    public static String next(String prefix) {
        synchronized (LOCK) {
            Path file = AppConfig.dataDir().resolve("counters.properties");
            Properties counters = new Properties();
            try {
                Files.createDirectories(file.getParent());
                if (Files.exists(file)) {
                    try (InputStream in = Files.newInputStream(file)) { counters.load(in); }
                }
                int next = Integer.parseInt(counters.getProperty(prefix, "1000")) + 1;
                counters.setProperty(prefix, String.valueOf(next));
                try (OutputStream out = Files.newOutputStream(file)) { counters.store(out, "ShopCore id counters"); }
                return prefix + next;
            } catch (IOException e) {
                throw new ShopCoreException("Could not generate id", e);
            }
        }
    }
}
