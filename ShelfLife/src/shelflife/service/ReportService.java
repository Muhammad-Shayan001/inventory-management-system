package shelflife.service;

import shelflife.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service contract for calculating dynamic operational reports,
 * KPI summaries, and audit analytics.
 */
public interface ReportService {

    void refreshData();

    int getTotalProductCount();
    int getTotalPhysicalStock();
    int getTotalSellableStock();
    int getLowStockCount();
    int getExpiredBatchCount();
    int getNearExpiryBatchCount(int daysThreshold);

    BigDecimal getSalesTotalForDate(LocalDate date);
    BigDecimal getTotalRevenue();

    List<Sale> getSalesHistory();
    List<ProductBatch> getStockInformation();
    List<WastageRecord> getWastageHistory();

    int getTotalWastageQuantity();
    BigDecimal getTotalWastageCost();

    BigDecimal getTotalProfit();
    BigDecimal getTotalRefunds();
    List<SaleReturn> getReturnHistory();
    List<Product> getReorderList();

    Product getProductById(String productId);

    // ---- analytics for the Business Overview dashboard ----

    /** Paid sales total per day for every day in [from, to] (days without sales are 0). */
    Map<LocalDate, BigDecimal> getDailySales(LocalDate from, LocalDate to);

    /** Paid revenue in [from, to]. */
    BigDecimal getRevenueForRange(LocalDate from, LocalDate to);

    /** Revenue minus cost of goods sold for paid sales in [from, to]. */
    BigDecimal getGrossProfitForRange(LocalDate from, LocalDate to);

    /** Average value of one paid sale in [from, to]; zero when there were none. */
    BigDecimal getAverageSaleValue(LocalDate from, LocalDate to);

    /** Best sellers by revenue in [from, to]. */
    List<ProductSales> getTopProducts(LocalDate from, LocalDate to, int limit);

    /** Paid revenue per payment method (display name) in [from, to]. */
    Map<String, BigDecimal> getSalesByPaymentMethod(LocalDate from, LocalDate to);

    /** Units wasted per reason in [from, to]. */
    Map<String, BigDecimal> getWastageByReason(LocalDate from, LocalDate to);

    /** Paid revenue per hour of day (0-23) in [from, to]. */
    Map<Integer, BigDecimal> getSalesByHour(LocalDate from, LocalDate to);

    /** Revenue and gross profit for the last {@code months} months, oldest first. */
    List<MonthlyRow> getMonthlySalesAndProfit(int months);

    /** On-hand units split into good / near-expiry / expired as of the given date. */
    StockHealth getStockHealth(LocalDate asOf, int nearExpiryDays);

    /** Value of all on-hand stock at batch cost price. */
    BigDecimal getStockValueAtCost();

    /** Active products that have stock but no paid sale in the last {@code days} days. */
    List<Product> getDeadStock(int days);
}
