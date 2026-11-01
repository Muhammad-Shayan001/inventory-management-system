package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Product catalog entity representing a saleable item definition.
 * Holds catalog metadata, unit selling price, and reorder threshold.
 */
public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal sellingPrice;
    private int lowStockThreshold;

    private String barcode;
    private String sku;
    private boolean active = true;

    public Product() {
        this.sellingPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.active = true;
    }

    public Product(String productId, String name, String category, BigDecimal sellingPrice, int lowStockThreshold) {
        this(productId, name, category, sellingPrice, lowStockThreshold, "", "", true);
    }

    public Product(String productId, String name, String category, BigDecimal sellingPrice, int lowStockThreshold, String barcode, String sku, boolean active) {
        setProductId(productId);
        setName(name);
        setCategory(category);
        setSellingPrice(sellingPrice);
        setLowStockThreshold(lowStockThreshold);
        this.barcode = barcode != null ? barcode.trim() : "";
        this.sku = sku != null ? sku.trim() : "";
        this.active = active;
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

    public String getBarcode() {
        return barcode != null ? barcode : "";
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode != null ? barcode.trim() : "";
    }

    public String getSku() {
        return sku != null ? sku : "";
    }

    public void setSku(String sku) {
        this.sku = sku != null ? sku.trim() : "";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSellable() {
        return active && sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isLowStock(int currentStock) {
        return currentStock <= this.lowStockThreshold;
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
        return productId + " - " + name + " (Rs " + (sellingPrice != null ? sellingPrice.toPlainString() : "0.00") + ")";
    }
}
