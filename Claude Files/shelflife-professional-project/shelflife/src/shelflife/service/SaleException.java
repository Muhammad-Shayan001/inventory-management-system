package shelflife.service;

/**
 * Exception thrown when a sales transaction cannot be committed due to
 * business validation errors (such as insufficient stock, expired stock, or invalid cart).
 */
public class SaleException extends Exception {

    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}
