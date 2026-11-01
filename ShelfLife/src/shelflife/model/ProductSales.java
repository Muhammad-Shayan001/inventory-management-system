package shelflife.model;

import java.math.BigDecimal;

/** Units sold, revenue and gross profit for one product over a period. */
public final class ProductSales {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal revenue;
    private final BigDecimal profit;

    public ProductSales(String productId, String productName, int quantity, BigDecimal revenue, BigDecimal profit) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.revenue = revenue;
        this.profit = profit;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getProfit() {
        return profit;
    }
}
