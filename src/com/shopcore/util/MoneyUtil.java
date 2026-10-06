package com.shopcore.util;

import com.shopcore.config.AppConfig;

import java.util.Locale;

public final class MoneyUtil {
    private MoneyUtil() {}

    /** Rounds to 2 decimals. (Production systems use BigDecimal; double keeps this project readable.) */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String format(double amount) {
        return AppConfig.CURRENCY_SYMBOL + String.format(Locale.US, "%,.2f", amount);
    }
}
