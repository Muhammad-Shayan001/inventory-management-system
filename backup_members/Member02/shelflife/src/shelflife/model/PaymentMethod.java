package shelflife.model;

/**
 * Standard payment methods supported by the ShelfLife billing system.
 * Payment details are recorded for accounting purposes only; no actual
 * payment gateway or card numbers are processed.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Credit/Debit Card"),
    DIGITAL_WALLET("Digital Wallet");

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
}
