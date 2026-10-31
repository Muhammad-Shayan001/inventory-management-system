package shelflife.storage;

import shelflife.model.*;
import java.util.List;

/**
 * Shared storage contract for ShelfLife.
 * Implemented by Member 4 (CSV persistence) and consumed by all module services.
 */
public interface DataStore {
    void loadAll() throws StorageException;
    void saveAll() throws StorageException;

    // --- Products ---
    List<Product> getAllProducts();
    Product getProductById(String productId);
    void addProduct(Product product) throws StorageException;
    void updateProduct(Product product) throws StorageException;
    void deleteProduct(String productId) throws StorageException;

    // --- Batches ---
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch) throws StorageException;
    void updateBatch(ProductBatch batch) throws StorageException;
    void deleteBatch(String batchId) throws StorageException;

    // --- Historical Sales & Items ---
    List<Sale> getAllSales();
    List<SaleItem> getAllSaleItems();
    List<WastageRecord> getAllWastageRecords();

    // --- Atomic Transactions ---
    void commitSale(Sale sale, List<SaleItem> items, List<ProductBatch> updatedBatches) throws StorageException;
    void commitWastage(WastageRecord wastage, List<ProductBatch> updatedBatches) throws StorageException;
}
