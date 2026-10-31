package shelflife.model;

/**
 * Lifecycle status of a sales transaction.
 */
public enum PaymentStatus {
    PAID("Paid"),
    PENDING("Pending"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static PaymentStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return PENDING;
        }
        String clean = text.trim();
        for (PaymentStatus ps : values()) {
            if (ps.name().equalsIgnoreCase(clean) || ps.displayName.equalsIgnoreCase(clean)) {
                return ps;
            }
        }
        return PENDING;
    }
}
