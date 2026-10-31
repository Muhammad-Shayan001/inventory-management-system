package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.WastageRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shared Report Service contract.
 * Used by DashboardReportsPanel to display inventory, sales, and wastage summaries.
 * All read operations; no persistence directly (delegated to DataStore).
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public interface ReportService {

    /**
     * Returns the total number of products in the catalog.
     */
    int getProductCount();

    /**
     * Returns the total physical stock across all batches (expired and non-expired).
     */
    int getTotalStock();

    /**
     * Returns the total sellable stock (non-expired, quantity > 0) across all products.
     */
    int getSellableStockTotal();

    /**
     * Returns the count of low-stock products (current quantity <= lowStockThreshold).
     */
    int getLowStockCount();

    /**
     * Returns the count of expired batches.
     */
    int getExpiredBatchCount();

    /**
     * Returns the count of near-expiry batches (within warning period).
     */
    int getNearExpiryCount(int days);

    /**
     * Returns total sales amount for a given day.
     * Returns BigDecimal.ZERO if no sales on that day.
     */
    BigDecimal getSalesTotalForDay(LocalDate date);

    /**
     * Returns sales history as a list of Sale summary records.
     */
    List<Sale> getSalesHistory();

    /**
     * Returns wastage history as a list of WastageRecord records.
     */
    List<WastageRecord> getWastageHistory();

    /**
     * Returns the total wastage quantity across all records.
     */
    int getTotalWastageQuantity();

    /**
     * Returns total wastage amount (if unit prices are tracked) or a summary value.
     */
    BigDecimal getTotalWastageAmount();
}