package shelflife.model;

/**
 * Payment status values for sales records.
 * Reflects the state of the payment, not the actual transaction result.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    CANCELLED,
    REFUNDED
}