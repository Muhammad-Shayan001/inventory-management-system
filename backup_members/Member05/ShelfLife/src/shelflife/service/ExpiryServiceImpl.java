package shelflife.service;

import shelflife.model.ProductBatch;
import shelflife.model.WastageRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standard implementation of ExpiryService using the shared InventoryService and DataStore.
 * Provides expiry queries and wastage recording.
 * 
 * @author ShelfLife Project (Member 5 / Integration Lead)
 * @version 1.0 (Shared Architecture Contract)
 */
public class ExpiryServiceImpl implements ExpiryService {
    private final InventoryService inventoryService;
    private final DataStore dataStore;

    public ExpiryServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        this.inventoryService = inventoryService;
        this.dataStore = dataStore;
    }

    @Override
    public List<ProductBatch> getExpiredBatches() {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.isExpired()) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public List<ProductBatch> getNearExpiryBatches(int days) {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate expiryCutoff = today.plusDays(days);
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired() && b.getQuantity() > 0 && !b.getExpiryDate().isBefore(today) && !b.getExpiryDate().isAfter(expiryCutoff)) {
                result.add(b);
            }
        }
        // Sort by expiry date ascending (FEFO order)
        result.sort(Comparator.comparing(ProductBatch::getExpiryDate).thenComparing(ProductBatch::getBatchId));
        return result;
    }

    @Override
    public List<ProductBatch> getEligibleBatches() {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired() && b.getQuantity() > 0) {
                result.add(b);
            }
        }
        // Sort by expiry date ascending (FEFO), then by batch ID as tie-breaker
        result.sort(Comparator.comparing(ProductBatch::getExpiryDate).thenComparing(ProductBatch::getBatchId));
        return result;
    }

    @Override
    public void recordWastage(WastageRecord record) throws Exception {
        // Validate that the batch exists
        String batchId = record.getBatchId();
        List<ProductBatch> batches = inventoryService.getBatchesForProduct(
                // We need the productId - for now, look up batches and find one matching
                // In a full implementation, the wastage record would carry the productId too
                batchId);
        
        ProductBatch batch = null;
        for (ProductBatch b : batches) {
            if (b.getBatchId().equals(batchId)) {
                batch = b;
                break;
            }
        }
        if (batch == null) {
            throw new Exception("Batch not found: " + batchId);
        }
        
        // Validate quantity
        if (record.getQuantity() <= 0) {
            throw new Exception("Wastage quantity must be positive.");
        }
        if (record.getQuantity() > batch.getQuantity()) {
            throw new Exception("Wastage quantity cannot exceed batch remaining quantity of " + batch.getQuantity());
        }
        
        // Set the wastage date to now if not set
        if (record.getWastageDateTime() == null) {
            record.setWastageDateTime(LocalDateTime.now());
        }
        
        // Add to data store
        List<WastageRecord> existingWastage = dataStore.loadWastageRecords();
        existingWastage.add(record);
        dataStore.saveWastageRecords(existingWastage);
        
        // Reduce batch quantity
        int newQuantity = batch.getQuantity() - record.getQuantity();
        if (newQuantity == 0) {
            // Remove batch if completely consumed
            inventoryService.deleteBatch(batchId);
        } else {
            inventoryService.updateBatchQuantity(batchId, newQuantity);
        }
    }

    @Override
    public List<WastageRecord> getWastageRecordsByBatchId(String batchId) {
        List<WastageRecord> result = new ArrayList<>();
        for (WastageRecord r : dataStore.loadWastageRecords()) {
            if (r.getBatchId().equals(batchId)) {
                result.add(r);
            }
        }
        return result;
    }
}