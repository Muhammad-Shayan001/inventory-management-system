package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import java.util.List;

/**
 * Shared InventoryService interface.
 * Consumed by Member 1 (ProductInventoryPanel), Member 2 (ExpiryAlertsPanel),
 * and Member 3 (BillingPanel / SalesService).
 */
public interface InventoryService {

    // --- Product Management ---
    List<Product> getAllProducts();
    Product getProductById(String productId);
    List<Product> searchProducts(String query);
    void addProduct(Product product);
    void updateProduct(Product product);
    boolean canDeleteProduct(String productId);
    void deleteProduct(String productId);

    // --- Batch Management ---
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch);
    void updateBatch(ProductBatch batch);
    void deleteBatch(String batchId);

    // --- Stock Metrics ---
    int getTotalStock(String productId);
    boolean isLowStock(String productId);

    // --- Synchronization ---
    void reload();
}
