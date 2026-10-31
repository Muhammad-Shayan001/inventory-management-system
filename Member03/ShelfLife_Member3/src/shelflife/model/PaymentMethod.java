package shelflife.model;

/**
 * Supported payment methods for sales transactions.
 * Note: These are for record-keeping only; no real payment gateway is integrated.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Credit / Debit Card"),
    UPI("UPI / Mobile Wallet"),
    OTHER("Other");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Parses a string representation into a PaymentMethod.
     * Defaults to CASH if unrecognized or null.
     */
    public static PaymentMethod fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CASH;
        }
        String clean = text.trim();
        for (PaymentMethod pm : values()) {
            if (pm.name().equalsIgnoreCase(clean) || pm.displayName.equalsIgnoreCase(clean)) {
                return pm;
            }
        }
        if (clean.toLowerCase().contains("card")) {
            return CARD;
        }
        if (clean.toLowerCase().contains("upi") || clean.toLowerCase().contains("wallet")) {
            return UPI;
        }
        return OTHER;
    }
}
