package com.shopcore.logging;

import com.shopcore.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Technical/application log (errors, warnings) -> data/shopcore.log. Not shown on the console. */
public final class AppLogger {
    private static Logger base;      // strong reference so the logger is not garbage collected

    private AppLogger() {}

    public static synchronized Logger get(Class<?> type) {
        if (base == null) init();
        return Logger.getLogger(type.getName());   // child of "com.shopcore", inherits the file handler
    }

    private static void init() {
        base = Logger.getLogger("com.shopcore");
        base.setUseParentHandlers(false);          // keep logs off the console UI
        base.setLevel(Level.INFO);
        try {
            Files.createDirectories(AppConfig.dataDir());
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %3$s - %5$s%6$s%n");
            FileHandler handler = new FileHandler(AppConfig.dataDir().resolve("shopcore.log").toString(), true);
            handler.setFormatter(new SimpleFormatter());
            handler.setEncoding("UTF-8");
            base.addHandler(handler);
        } catch (IOException e) {
            System.err.println("File logging disabled: " + e.getMessage());
        }
    }
}
