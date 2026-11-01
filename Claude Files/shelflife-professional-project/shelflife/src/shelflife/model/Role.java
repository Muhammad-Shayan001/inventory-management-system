package shelflife.model;

/**
 * System roles for user authentication and authorization.
 */
public enum Role {
    ADMIN("Administrator"),
    CASHIER("Cashier");

    private final String displayName;

    Role(String displayName) {
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
