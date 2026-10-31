package shelflife.service;

import shelflife.model.PaymentMethod;
import shelflife.model.Sale;
import shelflife.model.SaleItem;

import java.util.List;
import java.util.Map;

/**
 * Service contract for billing and sales management (Member 3).
 *
 * BUSINESS RULES:
 * - SalesService, not the GUI, owns final stock validation, FEFO allocation, and committing the sale.
 * - Revalidate stock at confirmation time; the cart's displayed quantities are not a guarantee of current stock.
 * - A SaleItem must identify the batch from which the units were allocated.
 * - When one product is allocated across several batches, save separate SaleItem records for those batch allocations.
 * - Store the actual unit price charged so historical receipts remain correct after the current product price changes.
 * - Payment method/status are records only; do not collect card numbers or process real card payments.
 * - Cancelled or failed attempts must not be stored as completed sales.
 */
public interface SalesService {
    /**
     * Atomically commits a sale:
     * 1. Revalidates non-expired sellable stock for each product.
     * 2. Allocates requested quantities across batches using FEFO (ascending expiry date).
     * 3. Creates Sale and SaleItem records with actual batch IDs and current unit prices.
     * 4. Updates inventory batch quantities.
     * 5. Atomically persists changes via the shared DataStore transaction.
     *
     * @param productQuantities Map of Product ID to requested quantity
     * @param paymentMethod Selected payment method
     * @return The committed Sale object
     * @throws SaleException if stock is insufficient, batch expired, or persistence fails
     */
    Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod) throws SaleException;

    /**
     * Overloaded method accepting payment method as String.
     */
    Sale commitSale(Map<String, Integer> productQuantities, String paymentMethod) throws SaleException;

    /**
     * Returns all completed sales transactions.
     */
    List<Sale> getAllSales();

    /**
     * Finds a sale by its unique ID.
     */
    Sale getSaleById(String saleId);

    /**
     * Retrieves all items associated with a particular sale ID.
     */
    List<SaleItem> getSaleItemsBySaleId(String saleId);

    /**
     * Reloads sales history from storage.
     */
    void reload();
}
