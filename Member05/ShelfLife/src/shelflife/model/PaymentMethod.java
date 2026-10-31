package shelflife.model;

/**
 * Payment methods available in the ShelfLife system.
 * Payment method/status are records only; no real card transaction is processed.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public enum PaymentMethod {
    CASH,
    CREDIT_CARD,
    DEBIT_CARD,
    MOBILE_PAYMENT,
    STORE_CREDIT
}