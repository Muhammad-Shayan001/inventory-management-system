package shelflife.model;

/**
 * Payment processing statuses recorded in sales transactions.
 */
public enum PaymentStatus {
    PAID("Paid"),
    PENDING("Pending"),
    CANCELLED("Cancelled");

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
}
