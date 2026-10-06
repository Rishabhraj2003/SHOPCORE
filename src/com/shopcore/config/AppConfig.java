package com.shopcore.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Central place for every constant and setting. Change behaviour here, not inside services. */
public final class AppConfig {
    private AppConfig() {}

    public static final String APP_NAME = "SHOPCORE";
    public static final String CURRENCY_SYMBOL = "₹";     // change to "Rs." if your console shows '?'

    public static final double TAX_RATE = 0.18;           // 18% GST
    public static final int LOW_STOCK_THRESHOLD = 5;
    public static final int MAX_QTY_PER_ITEM = 10;

    // Payment simulation limits (a payment above the limit is "declined")
    public static final double UPI_LIMIT = 100_000;
    public static final double CARD_LIMIT = 500_000;
    public static final double NET_BANKING_LIMIT = 1_000_000;
    public static final double COD_LIMIT = 50_000;

    // Data folder is changeable so tests/demos can use a temporary folder
    private static volatile Path dataDir = Paths.get("data");
    public static Path dataDir() { return dataDir; }
    public static void setDataDir(Path dir) { dataDir = dir; }
}
