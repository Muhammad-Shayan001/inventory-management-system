package shelflife.service;

/**
 * Exception thrown when a sales transaction violates business validation rules
 * (e.g., insufficient stock, expired batch, negative quantity, persistence failure).
 */
public class SaleException extends Exception {

    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}
