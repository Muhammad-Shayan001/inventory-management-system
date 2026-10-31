package shelflife.storage;

/**
 * Exception thrown when a storage operation fails.
 * Failed storage writes are reported and do not silently create partial records.
 */
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}