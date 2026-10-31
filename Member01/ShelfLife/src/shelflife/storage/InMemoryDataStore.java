package shelflife.storage;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe, in-memory implementation of DataStore for testing,
 * local validation, and development.
 */
public class InMemoryDataStore implements DataStore {
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, ProductBatch> batches = new LinkedHashMap<>();
    private final List<Sale> sales = new ArrayList<>();
    private final List<SaleItem> saleItems = new ArrayList<>();
    private final List<WastageRecord> wastageRecords = new ArrayList<>();

    @Override
    public synchronized void loadAll() throws StorageException {
        // No-op for in-memory implementation
    }

    @Override
    public synchronized void saveAll() throws StorageException {
        // No-op for in-memory implementation
    }

    @Override
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    @Override
    public synchronized Product getProductById(String productId) {
        if (productId == null) return null;
        return products.get(productId.trim().toUpperCase());
    }

    @Override
    public synchronized void addProduct(Product product) throws StorageException {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        String key = product.getProductId().trim().toUpperCase();
        if (products.containsKey(key)) {
            throw new StorageException("Product with ID " + key + " already exists.");
        }
        products.put(key, product);
    }

    @Override
    public synchronized void updateProduct(Product product) throws StorageException {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        String key = product.getProductId().trim().toUpperCase();
        if (!products.containsKey(key)) {
            throw new StorageException("Product with ID " + key + " does not exist.");
        }
        products.put(key, product);
    }

    @Override
    public synchronized void deleteProduct(String productId) throws StorageException {
        if (productId == null) throw new IllegalArgumentException("Product ID cannot be null.");
        String key = productId.trim().toUpperCase();
        if (!products.containsKey(key)) {
            throw new StorageException("Product with ID " + key + " does not exist.");
        }
        products.remove(key);
    }

    @Override
    public synchronized List<ProductBatch> getAllBatches() {
        return new ArrayList<>(batches.values());
    }

    @Override
    public synchronized List<ProductBatch> getBatchesByProductId(String productId) {
        if (productId == null) return Collections.emptyList();
        String key = productId.trim().toUpperCase();
        List<ProductBatch> result = new ArrayList<>();
        for (ProductBatch b : batches.values()) {
            if (key.equalsIgnoreCase(b.getProductId())) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public synchronized ProductBatch getBatchById(String batchId) {
        if (batchId == null) return null;
        return batches.get(batchId.trim().toUpperCase());
    }

    @Override
    public synchronized void addBatch(ProductBatch batch) throws StorageException {
        if (batch == null) throw new IllegalArgumentException("Batch cannot be null.");
        String key = batch.getBatchId().trim().toUpperCase();
        if (batches.containsKey(key)) {
            throw new StorageException("Batch with ID " + key + " already exists.");
        }
        batches.put(key, batch);
    }

    @Override
    public synchronized void updateBatch(ProductBatch batch) throws StorageException {
        if (batch == null) throw new IllegalArgumentException("Batch cannot be null.");
        if (!batches.containsKey(batch.getBatchId())) {
            throw new StorageException("Batch with ID " + batch.getBatchId() + " does not exist.");
        }
        batches.put(batch.getBatchId(), batch);
    }

    @Override
    public synchronized void deleteBatch(String batchId) throws StorageException {
        if (batchId == null) throw new IllegalArgumentException("Batch ID cannot be null.");
        if (!batches.containsKey(batchId.trim())) {
            throw new StorageException("Batch with ID " + batchId + " does not exist.");
        }
        batches.remove(batchId.trim());
    }

    @Override
    public synchronized List<Sale> getAllSales() {
        return new ArrayList<>(sales);
    }

    @Override
    public synchronized List<SaleItem> getAllSaleItems() {
        return new ArrayList<>(saleItems);
    }

    @Override
    public synchronized List<WastageRecord> getAllWastageRecords() {
        return new ArrayList<>(wastageRecords);
    }

    // Helper methods for populating test data and simulating historical transactions
    public synchronized void addTestSaleItem(SaleItem item) {
        this.saleItems.add(item);
    }

    public synchronized void addTestWastageRecord(WastageRecord record) {
        this.wastageRecords.add(record);
    }

    public synchronized void clear() {
        products.clear();
        batches.clear();
        sales.clear();
        saleItems.clear();
        wastageRecords.clear();
    }
}
