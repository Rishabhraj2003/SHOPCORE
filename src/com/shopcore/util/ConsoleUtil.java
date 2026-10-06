package com.shopcore.util;

import com.shopcore.exception.ShopCoreException;
import com.shopcore.logging.AppLogger;

import java.io.Console;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Every System.in / System.out helper lives here so controllers stay short. */
public final class ConsoleUtil {
    private static final Logger LOG = AppLogger.get(ConsoleUtil.class);
    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleUtil() {}

    // ---------- output ----------
    public static void header(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
    public static void line() { System.out.println("-".repeat(100)); }
    public static void success(String msg) { System.out.println("[OK] " + msg); }
    public static void error(String msg) { System.out.println("[ERROR] " + msg); }
    public static void info(String msg) { System.out.println("[INFO] " + msg); }

    // ---------- input ----------
    public static String readLine(String prompt) {
        System.out.print(prompt + ": ");
        System.out.flush();
        if (!SCANNER.hasNextLine()) {              // stdin closed (Ctrl+D or piped input ended)
            System.out.println();
            System.out.println("Input closed. Exiting.");
            System.exit(0);
        }
        return SCANNER.nextLine().trim();
    }

    public static String readRequired(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) return s;
            error("This field cannot be empty.");
        }
    }

    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] pw = console.readPassword(prompt + ": ");
            return pw == null ? "" : new String(pw);
        }
        return readLine(prompt);                   // IDE consoles have no hidden input
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
                error("Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                error("Please enter a valid number.");
            }
        }
    }

    public static double readDouble(String prompt, double min) {
        while (true) {
            String s = readLine(prompt);
            try {
                double v = Double.parseDouble(s);
                if (v >= min) return v;
                error("Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                error("Please enter a valid number.");
            }
        }
    }

    /** Returns null when the user just presses Enter. */
    public static Double readOptionalDouble(String prompt) {
        while (true) {
            String s = readLine(prompt + " (Enter to skip)");
            if (s.isEmpty()) return null;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                error("Please enter a valid number or press Enter.");
            }
        }
    }

    public static boolean confirm(String prompt) {
        String s = readLine(prompt + " (y/n)").toLowerCase();
        return s.equals("y") || s.equals("yes");
    }

    public static <E extends Enum<E>> E chooseEnum(String title, E[] values) {
        System.out.println(title + ":");
        for (int i = 0; i < values.length; i++) System.out.printf("  %d. %s%n", i + 1, values[i]);
        return values[readInt("Choose", 1, values.length) - 1];
    }

    /** Same as chooseEnum, but 0 means "skip" and returns null. */
    public static <E extends Enum<E>> E chooseEnumOptional(String title, E[] values) {
        System.out.println(title + ":");
        System.out.println("  0. Any / skip");
        for (int i = 0; i < values.length; i++) System.out.printf("  %d. %s%n", i + 1, values[i]);
        int choice = readInt("Choose", 0, values.length);
        return choice == 0 ? null : values[choice - 1];
    }

    /** Runs a menu action; business errors become friendly messages, bugs get logged. */
    public static void safely(Runnable action) {
        try {
            action.run();
        } catch (ShopCoreException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            error("Something went wrong: " + e.getMessage() + " (details saved in the log file)");
        }
    }
}
