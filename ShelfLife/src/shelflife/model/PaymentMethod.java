package shelflife.model;

/**
 * Payment method enumeration for point-of-sale recording.
 * Note: Payment method is purely an informational record field;
 * no actual card processing or external payment gateway is involved.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    EASYPAISA("Easypaisa"),
    JAZZCASH("JazzCash"),
    MOBILE_PAYMENT("Mobile Payment"),
    STORE_CREDIT("Store Credit");

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
