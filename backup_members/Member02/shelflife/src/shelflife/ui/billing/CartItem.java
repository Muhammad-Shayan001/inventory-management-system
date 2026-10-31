package shelflife.ui.billing;

import shelflife.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Temporary shopping cart item model used within BillingPanel.
 * Encapsulates selected catalog product and requested quantity before
 * final FEFO allocation is executed by SalesService.
 */
public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = Objects.requireNonNull(product, "product cannot be null");
        setQuantity(quantity);
    }

    public Product getProduct() {
        return product;
    }

    public String getProductId() {
        return product.getProductId();
    }

    public String getProductName() {
        return product.getName();
    }

    public BigDecimal getUnitPrice() {
        return product.getUnitPrice();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    public void addQuantity(int additional) {
        setQuantity(this.quantity + additional);
    }

    public BigDecimal getLineTotal() {
        return product.getUnitPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(product.getProductId(), cartItem.product.getProductId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(product.getProductId());
    }
}
