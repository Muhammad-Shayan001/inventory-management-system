package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;
import shelflife.storage.StorageException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultReportService implements ReportService {

    private final DataStore dataStore;

    public DefaultReportService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void refreshData() throws StorageException {
        dataStore.loadAll();
    }

    @Override
    public int getTotalProductCount() {
        return dataStore.getProducts().size();
    }

    @Override
    public int getTotalPhysicalStock() {
        int total = 0;
        for (ProductBatch b : dataStore.getBatches()) {
            total += b.getQuantity();
        }
        return total;
    }

    @Override
    public int getExpiredBatchesCount() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (ProductBatch b : dataStore.getBatches()) {
            if (b.getExpiryDate().isBefore(today)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getNearExpiryBatchesCount(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(daysThreshold);
        int count = 0;
        for (ProductBatch b : dataStore.getBatches()) {
            LocalDate exp = b.getExpiryDate();
            if (!exp.isBefore(today) && !exp.isAfter(future)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getLowStockProductCount() {
        Map<String, Integer> productStockMap = new HashMap<>();
        for (ProductBatch b : dataStore.getBatches()) {
            productStockMap.put(b.getProductId(), productStockMap.getOrDefault(b.getProductId(), 0) + b.getQuantity());
        }

        int lowStockCount = 0;
        for (Product p : dataStore.getProducts()) {
            int currentStock = productStockMap.getOrDefault(p.getProductId(), 0);
            if (currentStock <= p.getLowStockThreshold()) {
                lowStockCount++;
            }
        }
        return lowStockCount;
    }

    @Override
    public BigDecimal getSalesTotalForDate(LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Sale s : dataStore.getSales()) {
            if (s.getSaleDateTime().toLocalDate().equals(date)) {
                total = total.add(s.getTotalAmount());
            }
        }
        return total;
    }

    @Override public List<Sale> getSalesHistory() { return dataStore.getSales(); }
    @Override public List<ProductBatch> getStockInformation() { return dataStore.getBatches(); }
    @Override public List<WastageRecord> getWastageHistory() { return dataStore.getWastageRecords(); }

    @Override
    public Product getProductById(String productId) {
        for (Product p : dataStore.getProducts()) {
            if (p.getProductId().equals(productId)) {
                return p;
            }
        }
        return null;
    }
}
