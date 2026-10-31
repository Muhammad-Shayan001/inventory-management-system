package shelflife.service;

import shelflife.model.ProductBatch;
import shelflife.storage.DataStore;

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
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.isExpired()) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public int getExpiredBatchCount() {
        return getExpiredBatches().size();
    }

    @Override
    public List<ProductBatch> getNearExpiryBatches(int days) {
        List<ProductBatch> result = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate expiryCutoff = today.plusDays(days);
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired() && b.getQuantity() > 0 && !b.getExpiryDate().isBefore(today) && !b.getExpiryDate().isAfter(expiryCutoff)) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public int getNearExpiryCount(int days) {
        return getNearExpiryBatches(days).size();
    }
}