package com.shopcore.util;

import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");   // Indian mobile number

    private InputValidator() {}

    public static boolean isNotBlank(String s) { return s != null && !s.trim().isEmpty(); }

    public static boolean isValidEmail(String s) { return s != null && EMAIL.matcher(s.trim()).matches(); }

    public static boolean isValidPhone(String s) { return s != null && PHONE.matcher(s.trim()).matches(); }

    /** At least 8 characters, one letter and one digit. */
    public static boolean isStrongPassword(String s) {
        if (s == null || s.length() < 8) return false;
        boolean letter = false, digit = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return letter && digit;
    }
}
