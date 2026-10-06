package com.shopcore.util;

import com.shopcore.exception.ShopCoreException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() {}

    public static String format(LocalDateTime time) { return time.format(DATE_TIME); }

    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ShopCoreException("Date must be in yyyy-MM-dd format (example: 2027-12-31)");
        }
    }
}
