package shelflife.storage;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;

import java.util.List;

/**
 * Shared storage contract for ShelfLife.
 * Implemented by Member 4 (CSV persistence) and consumed by all module services.
 */
public interface DataStore {
    /**
     * Loads all records from permanent storage into memory.
     * @throws StorageException if loading fails.
     */
    void loadAll() throws StorageException;

    /**
     * Persists all in-memory records to permanent storage.
     * @throws StorageException if writing fails.
     */
    void saveAll() throws StorageException;

    // --- Product Operations ---
    List<Product> getAllProducts();
    Product getProductById(String productId);
    void addProduct(Product product) throws StorageException;
    void updateProduct(Product product) throws StorageException;
    void deleteProduct(String productId) throws StorageException;

    // --- Batch Operations ---
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch) throws StorageException;
    void updateBatch(ProductBatch batch) throws StorageException;
    void deleteBatch(String batchId) throws StorageException;

    // --- Historical / Downstream Data (for integrity checks and reporting) ---
    List<Sale> getAllSales();
    List<SaleItem> getAllSaleItems();
    List<WastageRecord> getAllWastageRecords();
}
