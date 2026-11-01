package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Standard thread-safe implementation of InventoryService backed by a shared DataStore.
 * Maintains memory cache for fast table rendering and transactional consistency.
 */
public class InventoryServiceImpl implements InventoryService {

    private final DataStore dataStore;
    private final Map<String, Product> productMap = new LinkedHashMap<>();
    private final Map<String, ProductBatch> batchMap = new LinkedHashMap<>();

    public InventoryServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public synchronized void reload() {
        try {
            productMap.clear();
            for (Product p : dataStore.loadProducts()) {
                productMap.put(p.getProductId().toUpperCase(), p);
            }
            batchMap.clear();
            for (ProductBatch b : dataStore.loadBatches()) {
                batchMap.put(b.getBatchId().toUpperCase(), b);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error reloading inventory data: " + e.getMessage());
        }
    }

    // =========================================================================
    // Product Operations
    // =========================================================================

    @Override
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(productMap.values());
    }

    @Override
    public synchronized Product getProductById(String productId) {
        if (productId == null) return null;
        return productMap.get(productId.trim().toUpperCase());
    }

    @Override
    public synchronized List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        String q = query.trim().toUpperCase();
        List<Product> results = new ArrayList<>();
        for (Product p : productMap.values()) {
            if (p.getProductId().toUpperCase().contains(q) ||
                p.getName().toUpperCase().contains(q) ||
                p.getCategory().toUpperCase().contains(q) ||
                (p.getBarcode() != null && p.getBarcode().toUpperCase().contains(q)) ||
                (p.getSku() != null && p.getSku().toUpperCase().contains(q))) {
                results.add(p);
            }
        }
        return results;
    }

    @Override
    public synchronized void addProduct(Product product) throws Exception {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        String id = product.getProductId().trim().toUpperCase();
        if (productMap.containsKey(id)) {
            throw new IllegalArgumentException("Product with ID '" + id + "' already exists.");
        }
        productMap.put(id, product);
        dataStore.saveProducts(new ArrayList<>(productMap.values()));
    }

    @Override
    public synchronized void updateProduct(Product product) throws Exception {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        String id = product.getProductId().trim().toUpperCase();
        if (!productMap.containsKey(id)) {
            throw new IllegalArgumentException("Product with ID '" + id + "' does not exist.");
        }
        productMap.put(id, product);
        dataStore.saveProducts(new ArrayList<>(productMap.values()));
    }

    @Override
    public synchronized boolean canDeleteProduct(String productId) {
        if (productId == null) return false;
        String id = productId.trim().toUpperCase();
        if (!productMap.containsKey(id)) return false;

        // Check if there are active batches
        for (ProductBatch b : batchMap.values()) {
            if (b.getProductId().equalsIgnoreCase(id) && b.getQuantity() > 0) {
                return false;
            }
        }

        // Check if historical sale items exist for this product
        try {
            List<SaleItem> saleItems = dataStore.loadSaleItems();
            for (SaleItem item : saleItems) {
                if (item.getProductId().equalsIgnoreCase(id)) {
                    return false;
                }
            }
        } catch (Exception ignored) {}

        return true;
    }

    @Override
    public synchronized void deleteProduct(String productId) throws Exception {
        if (productId == null) return;
        String id = productId.trim().toUpperCase();
        if (!productMap.containsKey(id)) {
            throw new IllegalArgumentException("Product with ID '" + id + "' does not exist.");
        }

        if (!canDeleteProduct(id)) {
            throw new IllegalStateException("Cannot delete product '" + id + "': dependent stock batches or historical sales exist.");
        }

        productMap.remove(id);
        dataStore.saveProducts(new ArrayList<>(productMap.values()));
    }

    // =========================================================================
    // Batch Operations
    // =========================================================================

    @Override
    public synchronized List<ProductBatch> getAllBatches() {
        return new ArrayList<>(batchMap.values());
    }

    @Override
    public synchronized List<ProductBatch> getBatchesForProduct(String productId) {
        return getBatchesByProductId(productId);
    }

    @Override
    public synchronized List<ProductBatch> getBatchesByProductId(String productId) {
        if (productId == null) return Collections.emptyList();
        String id = productId.trim().toUpperCase();
        List<ProductBatch> list = new ArrayList<>();
        for (ProductBatch b : batchMap.values()) {
            if (b.getProductId().equalsIgnoreCase(id)) {
                list.add(b);
            }
        }
        list.sort(Comparator.naturalOrder()); // FEFO order
        return list;
    }

    @Override
    public synchronized ProductBatch getBatchById(String batchId) {
        if (batchId == null) return null;
        return batchMap.get(batchId.trim().toUpperCase());
    }

    @Override
    public synchronized void addBatch(ProductBatch batch) throws Exception {
        if (batch == null) {
            throw new IllegalArgumentException("Product batch cannot be null.");
        }
        String batchId = batch.getBatchId().trim().toUpperCase();
        if (batchMap.containsKey(batchId)) {
            throw new IllegalArgumentException("Batch with ID '" + batchId + "' already exists.");
        }
        String prodId = batch.getProductId().trim().toUpperCase();
        if (!productMap.containsKey(prodId)) {
            throw new IllegalArgumentException("Cannot add batch: Product with ID '" + prodId + "' does not exist.");
        }

        batchMap.put(batchId, batch);
        dataStore.saveBatches(new ArrayList<>(batchMap.values()));
    }

    @Override
    public synchronized void updateBatch(ProductBatch batch) throws Exception {
        if (batch == null) {
            throw new IllegalArgumentException("Product batch cannot be null.");
        }
        String batchId = batch.getBatchId().trim().toUpperCase();
        if (!batchMap.containsKey(batchId)) {
            throw new IllegalArgumentException("Batch with ID '" + batchId + "' does not exist.");
        }
        batchMap.put(batchId, batch);
        dataStore.saveBatches(new ArrayList<>(batchMap.values()));
    }

    @Override
    public synchronized void updateBatchQuantity(String batchId, int newQuantity) throws Exception {
        if (batchId == null) return;
        ProductBatch b = getBatchById(batchId);
        if (b == null) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }
        b.setQuantity(newQuantity);
        dataStore.saveBatches(new ArrayList<>(batchMap.values()));
    }

    @Override
    public synchronized void deleteBatch(String batchId) throws Exception {
        if (batchId == null) return;
        String id = batchId.trim().toUpperCase();
        if (!batchMap.containsKey(id)) {
            throw new IllegalArgumentException("Batch with ID '" + id + "' does not exist.");
        }
        batchMap.remove(id);
        dataStore.saveBatches(new ArrayList<>(batchMap.values()));
    }

    // =========================================================================
    // Stock Calculations
    // =========================================================================

    @Override
    public synchronized int getTotalStock(String productId) {
        if (productId == null) return 0;
        int total = 0;
        for (ProductBatch b : getBatchesByProductId(productId)) {
            total += b.getQuantity();
        }
        return total;
    }

    @Override
    public synchronized int getSellableStock(String productId, LocalDate referenceDate) {
        if (productId == null) return 0;
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        int total = 0;
        for (ProductBatch b : getBatchesByProductId(productId)) {
            if (!b.isExpired(ref) && b.getQuantity() > 0) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    @Override
    public synchronized boolean isLowStock(String productId) {
        Product p = getProductById(productId);
        if (p == null) return false;
        int sellable = getSellableStock(productId, LocalDate.now());
        return p.isLowStock(sellable);
    }
}
