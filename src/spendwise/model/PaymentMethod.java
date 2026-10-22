package spendwise.model;

public enum PaymentMethod {
    UNSPECIFIED("Unspecified"),
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card");

    private final String displayName;
    PaymentMethod(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    public String toString() { return displayName; }
    public static PaymentMethod fromDisplayName(String value) {
        for (PaymentMethod method : values()) if (method.displayName.equals(value)) return method;
        throw new IllegalArgumentException("Unknown payment method: " + value);
    }
}
