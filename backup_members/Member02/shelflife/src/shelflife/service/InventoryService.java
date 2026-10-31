package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;

import java.time.LocalDate;
import java.util.List;

/**
 * Shared Inventory Service contract managed by Member 2 / Core.
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
     * Calculates the total sellable (non-expired and remaining quantity > 0)
     * stock for a given product as of the specified reference date.
     */
    int getSellableStock(String productId, LocalDate referenceDate);

    /**
     * Updates the remaining quantity of an inventory batch.
     */
    void updateBatchQuantity(String batchId, int newQuantity) throws Exception;

    /**
     * Reloads inventory data from underlying storage.
     */
    void reload();
}
