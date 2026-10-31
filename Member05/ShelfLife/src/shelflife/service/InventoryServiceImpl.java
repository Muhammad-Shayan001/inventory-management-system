package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.storage.DataStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard implementation of InventoryService that uses the shared DataStore
 * for persistent storage. Keeps in-memory state in sync with CSV files.
 * 
 * @author ShelfLife Project (Member 5 / Integration Lead)
 * @version 1.0 (Shared Architecture Contract)
 */
public class InventoryServiceImpl implements InventoryService {
    private final DataStore dataStore;
    private final List<Product> productsCache = new ArrayList<>();
    private final List<ProductBatch> batchesCache = new ArrayList<>();

    public InventoryServiceImpl(DataStore dataStore) {
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public List<Product> getAllProducts() {
        return new ArrayList<>(productsCache);
    }

    @Override
    public Product getProductById(String productId) {
        for (Product p : productsCache) {
            if (p.getProductId().equals(productId)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public List<ProductBatch> getBatchesForProduct(String productId) {
        List<ProductBatch> result = new ArrayList<>();
        for (ProductBatch b : batchesCache) {
            if (b.getProductId().equals(productId)) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public List<ProductBatch> getAllBatches() {
        return new ArrayList<>(batchesCache);
    }

    @Override
    public void deleteBatch(String batchId) throws Exception {
        batchesCache.removeIf(b -> b.getBatchId().equals(batchId));
        dataStore.saveBatches(new ArrayList<>(batchesCache));
    }

    @Override
    public int getSellableStock(String productId, LocalDate referenceDate) {
        int total = 0;
        for (ProductBatch b : batchesCache) {
            if (b.getProductId().equals(productId)
                    && !b.getExpiryDate().isBefore(referenceDate)
                    && b.getQuantity() > 0) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    @Override
    public void reload() {
        productsCache.clear();
        batchesCache.clear();
        try {
            List<Product> loadedProducts = dataStore.loadProducts();
            if (loadedProducts != null) {
                productsCache.addAll(loadedProducts);
            }
            List<ProductBatch> loadedBatches = dataStore.loadBatches();
            if (loadedBatches != null) {
                batchesCache.addAll(loadedBatches);
            }
        } catch (Exception e) {
            // If storage load fails, caches remain empty; GUI will show empty state
        }
    }

    /**
     * Adds a product to the catalog and persists it.
     */
    public void addProduct(Product product) throws Exception {
        // Check for duplicate product ID
        if (getProductById(product.getProductId()) != null) {
            throw new Exception("Product ID already exists: " + product.getProductId());
        }
        productsCache.add(product);
        dataStore.saveProducts(new ArrayList<>(productsCache));
    }

    /**
     * Updates a product in the catalog and persists it.
     */
    public void updateProduct(Product product) throws Exception {
        Product existing = getProductById(product.getProductId());
        if (existing == null) {
            throw new Exception("Product not found: " + product.getProductId());
        }
        // Replace old with updated
        productsCache.remove(existing);
        productsCache.add(product);
        dataStore.saveProducts(new ArrayList<>(productsCache));
    }

    /**
     * Deletes a product from the catalog.
     * @return true if deleted, false if product has associated batches
     * @throws Exception if deletion fails
     */
    public boolean deleteProduct(String productId) throws Exception {
        Product product = getProductById(productId);
        if (product == null) {
            return false;
        }
        // Check if product has associated batches
        List<ProductBatch> productBatches = getBatchesForProduct(productId);
        if (!productBatches.isEmpty()) {
            return false; // Cannot delete product with existing batches
        }
        productsCache.remove(product);
        dataStore.saveProducts(new ArrayList<>(productsCache));
        // Also delete associated batches
        for (ProductBatch batch : productBatches) {
            deleteBatch(batch.getBatchId());
        }
        return true;
    }

    @Override
    public void updateBatchQuantity(String batchId, int newQuantity) throws Exception {
        for (ProductBatch b : batchesCache) {
            if (b.getBatchId().equals(batchId)) {
                b.setQuantity(newQuantity);
                dataStore.saveBatches(new ArrayList<>(batchesCache));
                return;
            }
        }
        throw new Exception("Batch not found: " + batchId);
    }
}