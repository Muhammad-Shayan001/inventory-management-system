package shelflife.service;

import shelflife.model.PaymentMethod;
import shelflife.model.Sale;
import shelflife.model.SaleItem;

import java.util.List;
import java.util.Map;

/**
 * Service contract for sales processing, FEFO batch allocation,
 * and historical sale retrieval.
 */
public interface SalesService {

    /**
     * Commits a sale transaction with automatic FEFO stock allocation.
     * Re-validates stock at checkout time and guarantees atomic multi-file persistence.
     *
     * @param productQuantities Map of Product ID to requested quantity
     * @param paymentMethod     Selected payment method
     * @return The completed and persisted Sale object
     * @throws SaleException if stock is insufficient or cart is invalid
     */
    Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod) throws SaleException;

    /**
     * Overloaded commitSale accepting optional cashier notes.
     */
    Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod, String notes) throws SaleException;

    /**
     * Retrieves all historical sales.
     */
    List<Sale> getAllSales();

    /**
     * Retrieves a specific sale by its unique ID.
     */
    Sale getSaleById(String saleId);

    /**
     * Retrieves all line items associated with a specific sale ID.
     */
    List<SaleItem> getSaleItems(String saleId);

    default List<SaleItem> getSaleItemsBySaleId(String saleId) {
        return getSaleItems(saleId);
    }

    /**
     * Commits a sale with split payments, customer association, and custom discount.
     */
    Sale commitSaleWithPayments(Map<String, Integer> productQuantities, List<shelflife.model.PaymentLine> payments,
                                String customerId, java.math.BigDecimal discountAmount, String notes,
                                String cashierUsername) throws SaleException;

    /**
     * Returns product IDs of top-selling products over the past N days.
     */
    List<String> getTopSellingProductIds(int limit, int days);

    /**
     * Checks if selling a product at the given price/discount would be below its batch cost.
     */
    boolean isBelowCost(String productId, java.math.BigDecimal sellingPrice, java.math.BigDecimal discount);

    /**
     * Marks a receipt as reprinted (duplicate copy) and records audit details.
     */
    void markDuplicateReceipt(String saleId) throws Exception;

    /**
     * Logs a manual price override event with cashier and admin credentials.
     */
    void logPriceOverride(shelflife.model.PriceOverrideLog override) throws Exception;

    /**
     * Retrieves all split payment lines for a sale.
     */
    List<shelflife.model.PaymentLine> getPaymentLines(String saleId);

    /**
     * Reloads sales data from persistence.
     */
    void reload();
}
