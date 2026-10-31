package shelflife.service;

/**
 * Exception thrown when a sales transaction cannot be committed due to
 * business validation failure (e.g. insufficient stock, expired batches)
 * or transactional persistence failure.
 */
public class SaleException extends Exception {
    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}