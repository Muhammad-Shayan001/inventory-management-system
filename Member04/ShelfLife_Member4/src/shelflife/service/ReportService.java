package shelflife.service;

import shelflife.model.*;
import shelflife.storage.StorageException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    void refreshData() throws StorageException;

    int getTotalProductCount();
    int getTotalPhysicalStock();
    int getExpiredBatchesCount();
    int getNearExpiryBatchesCount(int daysThreshold);
    int getLowStockProductCount();

    BigDecimal getSalesTotalForDate(LocalDate date);

    List<Sale> getSalesHistory();
    List<ProductBatch> getStockInformation();
    List<WastageRecord> getWastageHistory();

    Product getProductById(String productId);
}
