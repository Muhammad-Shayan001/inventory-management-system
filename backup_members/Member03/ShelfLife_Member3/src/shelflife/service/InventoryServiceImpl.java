package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Standard implementation of InventoryService backed by DataStore.
 */
public class InventoryServiceImpl implements InventoryService {

    private final DataStore dataStore;

    public InventoryServiceImpl(DataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "DataStore cannot be null");
    }

    @Override
    public List<Product> getAllProducts() {
        return dataStore.getAllProducts();
    }

    @Override
    public Product getProductById(String productId) {
        if (productId == null) return null;
        return dataStore.getProductById(productId);
    }

    @Override
    public List<Product> searchProducts(String query) {
        List<Product> all = getAllProducts();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<Product> matches = new ArrayList<>();
        for (Product p : all) {
            if (p.getProductId().toLowerCase().contains(q) ||
                p.getName().toLowerCase().contains(q) ||
                p.getCategory().toLowerCase().contains(q)) {
                matches.add(p);
            }
        }
        return matches;
    }

    @Override
    public void addProduct(Product product) {
        dataStore.addProduct(product);
    }

    @Override
    public void updateProduct(Product product) {
        dataStore.updateProduct(product);
    }

    @Override
    public boolean canDeleteProduct(String productId) {
        List<ProductBatch> batches = getBatchesByProductId(productId);
        for (ProductBatch b : batches) {
            if (b.getQuantity() > 0) return false;
        }
        return true;
    }

    @Override
    public void deleteProduct(String productId) {
        dataStore.deleteProduct(productId);
    }

    @Override
    public List<ProductBatch> getAllBatches() {
        return dataStore.getAllBatches();
    }

    @Override
    public List<ProductBatch> getBatchesByProductId(String productId) {
        return dataStore.getBatchesByProductId(productId);
    }

    @Override
    public ProductBatch getBatchById(String batchId) {
        return dataStore.getBatchById(batchId);
    }

    @Override
    public void addBatch(ProductBatch batch) {
        dataStore.addBatch(batch);
    }

    @Override
    public void updateBatch(ProductBatch batch) {
        dataStore.updateBatch(batch);
    }

    @Override
    public void deleteBatch(String batchId) {
        dataStore.deleteBatch(batchId);
    }

    @Override
    public int getTotalStock(String productId) {
        List<ProductBatch> batches = getBatchesByProductId(productId);
        int total = 0;
        for (ProductBatch b : batches) {
            total += b.getQuantity();
        }
        return total;
    }

    @Override
    public boolean isLowStock(String productId) {
        Product p = getProductById(productId);
        if (p == null) return false;
        return getTotalStock(productId) <= p.getLowStockThreshold();
    }

    @Override
    public void reload() {
        dataStore.loadAll();
    }
}
