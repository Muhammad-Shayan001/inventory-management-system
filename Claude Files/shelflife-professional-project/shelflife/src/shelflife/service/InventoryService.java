package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.storage.DataStore;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for product catalog and batch inventory management.
 * Enforces business rules:
 * - Product IDs and batch IDs are unique.
 * - Every batch references an existing product.
 * - Quantities can never become negative.
 * - Products with active batches or historical records cannot be deleted.
 */
public interface InventoryService {

    // --- Product Operations ---
    List<Product> getAllProducts();
    Product getProductById(String productId);
    List<Product> searchProducts(String query);
    void addProduct(Product product) throws Exception;
    void updateProduct(Product product) throws Exception;
    boolean canDeleteProduct(String productId);
    void deleteProduct(String productId) throws Exception;

    default List<Product> getActiveProducts() {
        List<Product> active = new java.util.ArrayList<>();
        for (Product p : getAllProducts()) {
            if (p.isActive()) {
                active.add(p);
            }
        }
        return active;
    }

    default void setProductActive(String productId, boolean active) throws Exception {
        Product p = getProductById(productId);
        if (p != null) {
            p.setActive(active);
            updateProduct(p);
        }
    }

    // --- Batch Operations ---
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesForProduct(String productId);
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch) throws Exception;
    void updateBatch(ProductBatch batch) throws Exception;
    void updateBatchQuantity(String batchId, int newQuantity) throws Exception;
    void deleteBatch(String batchId) throws Exception;

    // --- Stock Metrics ---
    int getTotalStock(String productId);
    int getSellableStock(String productId, LocalDate referenceDate);
    boolean isLowStock(String productId);

    // --- Persistence & Sync ---
    void reload();
    DataStore getDataStore();
}
