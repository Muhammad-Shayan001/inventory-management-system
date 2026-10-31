package shelflife.test;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.service.InventoryService;
import shelflife.storage.DataStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test implementation of InventoryService backed by DataStore.
 * Mimics Member 2's service to facilitate rigorous unit & integration testing.
 */
public class MockInventoryService implements InventoryService {
    private final DataStore dataStore;
    private final List<Product> products = new ArrayList<>();
    private final List<ProductBatch> batches = new ArrayList<>();

    public MockInventoryService(DataStore dataStore) {
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(products);
    }

    @Override
    public synchronized Product getProductById(String productId) {
        for (Product p : products) {
            if (p.getProductId().equalsIgnoreCase(productId)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized List<ProductBatch> getBatchesForProduct(String productId) {
        List<ProductBatch> result = new ArrayList<>();
        for (ProductBatch b : batches) {
            if (b.getProductId().equalsIgnoreCase(productId)) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public synchronized int getSellableStock(String productId, LocalDate referenceDate) {
        int total = 0;
        for (ProductBatch b : batches) {
            if (b.getProductId().equalsIgnoreCase(productId) && !b.isExpired(referenceDate) && b.getQuantity() > 0) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    @Override
    public synchronized void updateBatchQuantity(String batchId, int newQuantity) throws Exception {
        for (ProductBatch b : batches) {
            if (b.getBatchId().equalsIgnoreCase(batchId)) {
                b.setQuantity(newQuantity);
                break;
            }
        }
        dataStore.saveBatches(batches);
    }

    @Override
    public synchronized void reload() {
        try {
            products.clear();
            batches.clear();
            products.addAll(dataStore.loadProducts());
            batches.addAll(dataStore.loadBatches());
        } catch (Exception e) {
            throw new RuntimeException("Failed to reload inventory from dataStore", e);
        }
    }
}
