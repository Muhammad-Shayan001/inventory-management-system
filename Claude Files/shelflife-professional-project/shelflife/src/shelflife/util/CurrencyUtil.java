package shelflife.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for uniform PKR currency formatting and financial rounding.
 * All monetary display must go through this class to ensure consistency.
 *
 * Formats:
 *   format(BigDecimal)        -> "Rs. 1,250.00"
 *   formatCompact(BigDecimal) -> "1,250.00"   (no prefix, for receipts/reports)
 *   formatPKR(BigDecimal)     -> "Rs. 1,250.00" (alias for format)
 */
public final class CurrencyUtil {

    private static final DecimalFormat PKR_FORMAT;
    private static final DecimalFormat COMPACT_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        PKR_FORMAT    = new DecimalFormat("#,##0.00", symbols);
        COMPACT_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private CurrencyUtil() {}

    // -------------------------------------------------------------------------
    // Primary formatting methods (used by newer billing/receipt code)
    // -------------------------------------------------------------------------

    /**
     * Formats a BigDecimal as "Rs. 1,250.00"
     */
    public static String format(BigDecimal amount) {
        if (amount == null) return "Rs. 0.00";
        return "Rs. " + PKR_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Formats a BigDecimal without the "Rs." prefix — useful for receipt columns.
     * Example: 1250.00
     */
    public static String formatCompact(BigDecimal amount) {
        if (amount == null) return "0.00";
        return COMPACT_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Formats a plain double as "Rs. 1,250.00"
     */
    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    // -------------------------------------------------------------------------
    // Legacy aliases kept for backward compatibility
    // -------------------------------------------------------------------------

    /** Legacy alias — identical to format(BigDecimal). */
    public static String formatPKR(BigDecimal amount) {
        return format(amount);
    }

    /** Legacy alias — formats a double. */
    public static String formatPKR(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    // -------------------------------------------------------------------------
    // Financial rounding
    // -------------------------------------------------------------------------

    /**
     * Standardizes a BigDecimal to 2 decimal places with HALF_UP rounding.
     * Use this for all intermediate financial calculations.
     */
    public static BigDecimal roundMoney(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Safe subtraction — returns (a - b), both rounded to 2dp.
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal sa = a != null ? a : BigDecimal.ZERO;
        BigDecimal sb = b != null ? b : BigDecimal.ZERO;
        return roundMoney(sa.subtract(sb));
    }

    /**
     * Safe addition — returns (a + b), both rounded to 2dp.
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal sa = a != null ? a : BigDecimal.ZERO;
        BigDecimal sb = b != null ? b : BigDecimal.ZERO;
        return roundMoney(sa.add(sb));
    }

    /**
     * Safe multiply — returns (a * b), rounded to 2dp.
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        BigDecimal sa = a != null ? a : BigDecimal.ZERO;
        BigDecimal sb = b != null ? b : BigDecimal.ZERO;
        return roundMoney(sa.multiply(sb));
    }

    /**
     * Returns ZERO if amount is null, else amount.
     */
    public static BigDecimal orZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
