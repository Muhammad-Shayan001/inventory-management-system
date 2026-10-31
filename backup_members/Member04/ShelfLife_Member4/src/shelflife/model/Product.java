package shelflife.model;

import java.math.BigDecimal;

public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal sellingPrice;
    private int lowStockThreshold;

    public Product(String productId, String name, String category, BigDecimal sellingPrice, int lowStockThreshold) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
}
