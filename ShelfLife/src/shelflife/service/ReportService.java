package shelflife.service;

import shelflife.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
}
