package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a master product catalog item in the ShelfLife system.
 * Maintained by Member 2 / Shared Architecture.
 */
public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal unitPrice;

    public Product() {
        this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Product(String productId, String name, String category, BigDecimal unitPrice) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        setUnitPrice(unitPrice);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public String toString() {
        return "[" + productId + "] " + name + " ($" + (unitPrice != null ? unitPrice.toPlainString() : "0.00") + ")";
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
}
