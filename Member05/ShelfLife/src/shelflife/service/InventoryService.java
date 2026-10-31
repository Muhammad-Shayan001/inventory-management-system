package shelflife.service;

import java.util.List;
import shelflife.model.Product;
import shelflife.model.ProductBatch;
import java.time.LocalDate;

/**
 * Shared Inventory Service contract.
 * Used by ProductInventoryPanel, ExpiryAlertsPanel, and BillingPanel.
 * Business rules: Product IDs and batch IDs are unique.
 * Every batch references an existing product.
 * Quantity never becomes negative.
 * Expired batches are excluded from normal sale allocation.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public interface InventoryService {

    /**
     * Retrieves all products registered in the inventory.
     */
    List<Product> getAllProducts();

    /**
     * Looks up a product by unique product ID.
     */
    Product getProductById(String productId);

    /**
     * Retrieves all batches associated with a product.
     */
    List<ProductBatch> getBatchesForProduct(String productId);

    /**
     * Retrieves all batches in the inventory.
     */
    List<ProductBatch> getAllBatches();

    /**
     * Deletes a batch from the inventory.
     */
    void deleteBatch(String batchId) throws Exception;

    /**
     * Calculates the total sellable (non-expired and remaining quantity > 0)
     * stock for a given product as of the specified reference date.
     * Expired batches are excluded from the count.
     */
    int getSellableStock(String productId, LocalDate referenceDate);

    /**
     * Updates the remaining quantity of an inventory batch.
     * Throws Exception if the new quantity would be negative.
     */
    void updateBatchQuantity(String batchId, int newQuantity) throws Exception;

    /**
     * Reloads inventory data from underlying storage.
     * Should be called after any persistent change to keep GUI in sync.
     */
    void reload();

    /**
     * Adds a product to the catalog and persists it.
     * Throws Exception if a product with the same ID already exists.
     */
    void addProduct(Product product) throws Exception;

    /**
     * Updates a product in the catalog and persists it.
     * Throws Exception if the product is not found.
     */
    void updateProduct(Product product) throws Exception;

    /**
     * Deletes a product from the catalog.
     * Returns true if deleted, false if product has associated batches.
     * Throws Exception if deletion fails.
     */
    boolean deleteProduct(String productId) throws Exception;
}
