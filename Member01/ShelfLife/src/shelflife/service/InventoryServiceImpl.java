package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;
import shelflife.storage.DataStore;
import shelflife.storage.StorageException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of InventoryService that coordinates with DataStore
 * and enforces all business validation rules for products and batches.
 */
public class InventoryServiceImpl implements InventoryService {

    private final DataStore dataStore;

    public InventoryServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
    }

    // =========================================================================
    // Product Management
    // =========================================================================

    @Override
    public List<Product> getAllProducts() {
        return dataStore.getAllProducts();
    }

    @Override
    public Product getProductById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return null;
        }
        return dataStore.getProductById(productId.trim());
    }

    @Override
    public List<Product> searchProducts(String query) {
        List<Product> all = getAllProducts();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }

        String term = query.trim().toLowerCase();
        List<Product> matches = new ArrayList<>();
        for (Product p : all) {
            boolean idMatch = p.getProductId() != null && p.getProductId().toLowerCase().contains(term);
            boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(term);
            boolean catMatch = p.getCategory() != null && p.getCategory().toLowerCase().contains(term);
            if (idMatch || nameMatch || catMatch) {
                matches.add(p);
            }
        }
        return matches;
    }

    @Override
    public void addProduct(Product product) {
        validateProduct(product);

        if (getProductById(product.getProductId()) != null) {
            throw new IllegalArgumentException("Product ID '" + product.getProductId() + "' already exists.");
        }

        dataStore.addProduct(product);
    }

    @Override
    public void updateProduct(Product product) {
        validateProduct(product);

        if (getProductById(product.getProductId()) == null) {
            throw new IllegalArgumentException("Cannot update: Product ID '" + product.getProductId() + "' does not exist.");
        }

        dataStore.updateProduct(product);
    }

    @Override
    public boolean canDeleteProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String cleanId = productId.trim();

        // 1. Check if product exists
        if (getProductById(cleanId) == null) {
            return false;
        }

        // 2. Reject if any active/existing batch belongs to this product
        List<ProductBatch> batches = dataStore.getBatchesByProductId(cleanId);
        if (!batches.isEmpty()) {
            return false;
        }

        // 3. Reject if any historical sale item references this product
        List<SaleItem> saleItems = dataStore.getAllSaleItems();
        if (saleItems != null) {
            for (SaleItem item : saleItems) {
                if (cleanId.equalsIgnoreCase(item.getProductId())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void deleteProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        String cleanId = productId.trim();

        if (getProductById(cleanId) == null) {
            throw new IllegalArgumentException("Product ID '" + cleanId + "' does not exist.");
        }

        if (!canDeleteProduct(cleanId)) {
            throw new IllegalStateException("Cannot delete product '" + cleanId +
                    "': It has associated batches or historical records that depend on it.");
        }

        dataStore.deleteProduct(cleanId);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (product.getProductId() == null || product.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category is required.");
        }
        if (product.getSellingPrice() == null) {
            throw new IllegalArgumentException("Selling price is required.");
        }
        if (product.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Selling price must be strictly greater than zero.");
        }
        if (product.getLowStockThreshold() < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative.");
        }
    }

    // =========================================================================
    // Batch Management
    // =========================================================================

    @Override
    public List<ProductBatch> getAllBatches() {
        return dataStore.getAllBatches();
    }

    @Override
    public List<ProductBatch> getBatchesByProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return dataStore.getBatchesByProductId(productId.trim());
    }

    @Override
    public ProductBatch getBatchById(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            return null;
        }
        return dataStore.getBatchById(batchId.trim());
    }

    @Override
    public void addBatch(ProductBatch batch) {
        validateBatch(batch);

        // 1. Must reference an existing product
        if (getProductById(batch.getProductId()) == null) {
            throw new IllegalArgumentException("Cannot add batch: Product ID '" + batch.getProductId() + "' does not exist.");
        }

        // 2. Reject duplicate batch ID
        if (getBatchById(batch.getBatchId()) != null) {
            throw new IllegalArgumentException("Batch ID '" + batch.getBatchId() + "' already exists.");
        }

        dataStore.addBatch(batch);
    }

    @Override
    public void updateBatch(ProductBatch batch) {
        validateBatch(batch);

        if (getProductById(batch.getProductId()) == null) {
            throw new IllegalArgumentException("Cannot update batch: Product ID '" + batch.getProductId() + "' does not exist.");
        }

        if (getBatchById(batch.getBatchId()) == null) {
            throw new IllegalArgumentException("Cannot update: Batch ID '" + batch.getBatchId() + "' does not exist.");
        }

        dataStore.updateBatch(batch);
    }

    @Override
    public void deleteBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty.");
        }
        String cleanId = batchId.trim();

        if (getBatchById(cleanId) == null) {
            throw new IllegalArgumentException("Batch ID '" + cleanId + "' does not exist.");
        }

        // Check if historical sale items reference this batch
        List<SaleItem> saleItems = dataStore.getAllSaleItems();
        if (saleItems != null) {
            for (SaleItem item : saleItems) {
                if (cleanId.equalsIgnoreCase(item.getBatchId())) {
                    throw new IllegalStateException("Cannot delete batch '" + cleanId +
                            "': Historical sales records depend on it.");
                }
            }
        }

        // Check if historical wastage references this batch
        List<WastageRecord> waste = dataStore.getAllWastageRecords();
        if (waste != null) {
            for (WastageRecord wr : waste) {
                if (cleanId.equalsIgnoreCase(wr.getBatchId())) {
                    throw new IllegalStateException("Cannot delete batch '" + cleanId +
                            "': Historical wastage records depend on it.");
                }
            }
        }

        dataStore.deleteBatch(cleanId);
    }

    private void validateBatch(ProductBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch cannot be null.");
        }
        if (batch.getBatchId() == null || batch.getBatchId().trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID is required.");
        }
        if (batch.getProductId() == null || batch.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID is required for batch.");
        }
        if (batch.getQuantity() <= 0) {
            throw new IllegalArgumentException("Initial batch quantity must be greater than zero.");
        }
        if (batch.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required.");
        }
    }

    // =========================================================================
    // Stock Calculations & Status
    // =========================================================================

    @Override
    public int getTotalStock(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return 0;
        }
        List<ProductBatch> batches = dataStore.getBatchesByProductId(productId.trim());
        int total = 0;
        for (ProductBatch b : batches) {
            if (b.getQuantity() > 0) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    @Override
    public boolean isLowStock(String productId) {
        Product p = getProductById(productId);
        if (p == null) {
            return false;
        }
        return getTotalStock(productId) <= p.getLowStockThreshold();
    }
}
