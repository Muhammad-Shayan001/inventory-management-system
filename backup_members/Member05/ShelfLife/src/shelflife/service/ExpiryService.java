package shelflife.service;

import shelflife.model.ProductBatch;
import java.time.LocalDate;
import java.util.List;

/**
 * Shared Expiry Service contract.
 * Used by ExpiryAlertsPanel alongside InventoryService.
 * Provides expiry-related queries and wastage recording.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public interface ExpiryService {

    /**
     * Returns all batches that are expired (expiryDate is before today).
     * A batch expiring today is NOT considered expired per project rule.
     */
    List<ProductBatch> getExpiredBatches();

    /**
     * Returns all batches approaching expiry within the specified warning period (inclusive).
     * Batches whose expiry date is today or within the next {@code days} days.
     * Excludes batches with zero quantity.
     */
    List<ProductBatch> getNearExpiryBatches(int days);

    /**
     * Returns all batches with positive quantity that are not expired,
     * suitable for FEFO-based sale allocation.
     */
    List<ProductBatch> getEligibleBatches();

    /**
     * Records a wastage event for the specified batch.
     * Wastage quantity must be positive and cannot exceed the batch's remaining quantity.
     * Throws Exception if validation fails.
     */
    void recordWastage(WastageRecord record) throws Exception;

    /**
     * Returns all wastage records for a given batch ID.
     */
    List<WastageRecord> getWastageRecordsByBatchId(String batchId);
}