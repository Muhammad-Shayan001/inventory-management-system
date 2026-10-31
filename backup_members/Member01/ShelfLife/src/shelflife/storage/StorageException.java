package shelflife.storage;

/**
 * Exception thrown when a persistence or file-storage operation fails.
 * Designed to provide user-understandable failure descriptions.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
