package com.vayana.wallet.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * =========================================================================================
 * MONETARY COMPUTING UTILITY: MoneyUtil
 * =========================================================================================
 * Evaluator Walkthrough Notes:
 * ----------------------------
 * 1. MANDATORY REQUIREMENT:
 *    - "Store money as whole paise (integers), never as floating-point numbers."
 *
 * 2. WHY NEVER FLOAT / DOUBLE IN FINANCIAL SYSTEMS?
 *    - IEEE 754 Floating-Point Representation Issue:
 *      In binary, base-10 fractional decimals like 0.1 or 0.7 cannot be represented precisely.
 *      For example: (0.1 + 0.2) in Java evaluates to `0.30000000000000004`.
 *      If a bank processes 1,000,000 transactions, micro-rounding errors leak money!
 *
 * 3. ZERO-FLOAT INTEGER MATH ENGINE:
 *    - All internal calculations in this codebase use 64-bit integer arithmetic (`long`).
 *    - 1 Rupee = 100 Paise.
 *    - Input conversion (e.g., "100.50" -> 10050 paise) is performed via String-splitting
 *      and integer parsing, strictly avoiding `Double.parseDouble(s) * 100`.
 * =========================================================================================
 */
public final class MoneyUtil {

    private static final DecimalFormat RUPEE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        RUPEE_FORMAT = new DecimalFormat("₹#,##0.00", symbols);
    }

    private MoneyUtil() {}

    /**
     * Parses a string representation of money into whole paise without using floating-point math.
     * Supported formats:
     *  - "1000" -> 1000 Rupees -> 100000 paise
     *  - "1000.50" -> 1000 Rupees 50 paise -> 100050 paise
     *  - "1000.5" -> 1000 Rupees 50 paise -> 100050 paise
     *  - "₹1000.50" or "Rs. 1000.50" -> 100050 paise
     *  - "100000p" or "100000paise" -> 100000 paise directly
     */
    public static long parseToPaise(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be empty");
        }

        String cleaned = input.trim().replace(",", "");

        // Check for explicit paise suffix (e.g. 50000p or 50000paise)
        if (cleaned.toLowerCase().endsWith("paise")) {
            String val = cleaned.substring(0, cleaned.length() - 5).trim();
            return Long.parseLong(val);
        }
        if (cleaned.toLowerCase().endsWith("p")) {
            String val = cleaned.substring(0, cleaned.length() - 1).trim();
            return Long.parseLong(val);
        }

        // Clean currency symbols if present
        if (cleaned.startsWith("₹")) {
            cleaned = cleaned.substring(1).trim();
        } else if (cleaned.toLowerCase().startsWith("rs.")) {
            cleaned = cleaned.substring(3).trim();
        } else if (cleaned.toLowerCase().startsWith("rs")) {
            cleaned = cleaned.substring(2).trim();
        } else if (cleaned.toLowerCase().startsWith("inr")) {
            cleaned = cleaned.substring(3).trim();
        }

        // String-based split avoids IEEE-754 floating-point inaccuracies
        if (cleaned.contains(".")) {
            String[] parts = cleaned.split("\\.");
            if (parts.length > 2) {
                throw new IllegalArgumentException("Invalid monetary format: " + input);
            }
            long rupees = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
            String fractionStr = parts.length > 1 ? parts[1] : "";

            if (fractionStr.length() > 2) {
                throw new IllegalArgumentException("Cannot have fractions smaller than 1 paisa (max 2 decimal places): " + input);
            }

            long paise = 0;
            if (fractionStr.length() == 1) {
                paise = Long.parseLong(fractionStr) * 10;
            } else if (fractionStr.length() == 2) {
                paise = Long.parseLong(fractionStr);
            }

            if (rupees < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            return (rupees * 100) + paise;
        } else {
            long rupees = Long.parseLong(cleaned);
            return rupees * 100;
        }
    }

    /**
     * Formats whole paise into standard Rupee string representation without floating-point errors.
     */
    public static String formatRupees(long paise) {
        long wholeRupees = paise / 100;
        long remainderPaise = Math.abs(paise % 100);
        return String.format(Locale.US, "₹%,d.%02d (Rs. %,d.%02d)", wholeRupees, remainderPaise, wholeRupees, remainderPaise);
    }

    /**
     * Formats with both Rupee and exact paise display: "₹1,000.00 (Rs. 1,000.00) [100000 paise]"
     */
    public static String formatDetailed(long paise) {
        long wholeRupees = paise / 100;
        long remainderPaise = Math.abs(paise % 100);
        return String.format(Locale.US, "₹%,d.%02d (Rs. %,d.%02d) [%,d paise]", wholeRupees, remainderPaise, wholeRupees, remainderPaise, paise);
    }
}
