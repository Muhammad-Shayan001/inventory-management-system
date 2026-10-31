package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a product in the ShelfLife inventory catalog.
 * Product IDs and batch IDs are unique. Quantity never becomes negative.
 * Expired batches are excluded from normal sale allocation.
 * Category and name must not be blank. Selling price must be positive.
 * Low stock threshold must be non-negative.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal sellingPrice;
    private int lowStockThreshold;

    public Product() {
        this.sellingPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lowStockThreshold = 0;
    }

    public Product(String productId, String name, String category, BigDecimal sellingPrice, int lowStockThreshold) {
        setProductId(productId);
        setName(name);
        setCategory(category);
        setSellingPrice(sellingPrice);
        setLowStockThreshold(lowStockThreshold);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        this.productId = productId.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty.");
        }
        this.category = category.trim();
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        if (sellingPrice == null) {
            throw new IllegalArgumentException("Selling price cannot be null.");
        }
        if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Selling price must be greater than zero.");
        }
        this.sellingPrice = sellingPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative.");
        }
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock(int currentQuantity) {
        return currentQuantity <= lowStockThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return productId + " - " + name + " (" + category + ") @ " + sellingPrice.toPlainString();
    }
}