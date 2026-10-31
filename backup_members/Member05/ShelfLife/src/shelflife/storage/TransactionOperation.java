package shelflife.storage;

/**
 * Functional callback interface representing an atomic transactional unit of work.
 * Used by DataStore.executeTransaction() for rollback-recovery.
 */
@FunctionalInterface
public interface TransactionOperation {
    void execute() throws Exception;
}