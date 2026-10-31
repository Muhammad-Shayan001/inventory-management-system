package shelflife.storage;

/**
 * Functional callback interface representing an atomic transactional unit of work.
 */
@FunctionalInterface
public interface TransactionOperation {
    void execute() throws Exception;
}
