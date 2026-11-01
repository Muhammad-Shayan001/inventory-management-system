package shelflife.service;

import shelflife.model.ProductBatch;
import shelflife.model.WastageRecord;

import java.util.List;

/**
 * Service contract for managing product batch expiration tracking,
 * warning thresholds, and recording stock wastage.
 */
public interface ExpiryService {

    /**
     * Retrieves all batches that are currently expired (expiryDate is before today).
     */
    List<ProductBatch> getExpiredBatches();

    /**
     * Retrieves non-expired batches expiring within the specified days threshold.
     */
    List<ProductBatch> getNearExpiryBatches(int days);

    /**
     * Retrieves all active, non-expired batches for a product sorted by FEFO.
     */
    List<ProductBatch> getEligibleBatches(String productId);

    /**
     * Records inventory wastage, deducts the wasted quantity from the batch,
     * and persists the transaction.
     */
    void recordWastage(WastageRecord record) throws Exception;

    /**
     * Retrieves historical wastage records associated with a specific batch.
     */
    List<WastageRecord> getWastageRecordsByBatchId(String batchId);

    /**
     * Retrieves all historical wastage records.
     */
    List<WastageRecord> getAllWastageRecords();

    /**
     * Recalls a batch from sale, preventing any future checkout.
     */
    void recallBatch(String batchId, String reason, String adminUsername) throws Exception;

    /**
     * Unrecalls a batch, returning it to sellable status.
     */
    void unrecallBatch(String batchId, String adminUsername) throws Exception;

    /**
     * Returns suggested clearance discount percentage based on days until expiry.
     */
    int getSuggestedClearanceDiscount(ProductBatch batch);

    /**
     * Calculates the waste risk category for a batch (HIGH, MEDIUM, LOW) based on sales velocity.
     */
    String calculateBatchWasteRisk(ProductBatch batch);

    /**
     * Returns full traceability information for a batch (supplier, intake, sales, wastage).
     */
    java.util.Map<String, Object> getBatchTraceability(String batchId);
}
