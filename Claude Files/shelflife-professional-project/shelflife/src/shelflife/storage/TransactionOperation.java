package shelflife.storage;

/**
 * Functional interface representing an operation executed within an atomic storage transaction.
 */
@FunctionalInterface
public interface TransactionOperation {
    void execute() throws Exception;
}
