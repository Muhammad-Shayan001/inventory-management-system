package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.WastageRecord;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {
    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final DataStore dataStore;

    public ReportServiceImpl(InventoryService inventoryService, SalesService salesService, DataStore dataStore) {
        this.inventoryService = inventoryService;
        this.salesService = salesService;
        this.dataStore = dataStore;
    }

    @Override
    public int getProductCount() {
        return inventoryService.getAllProducts().size();
    }

    @Override
    public int getTotalStock() {
        int total = 0;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            total += b.getQuantity();
        }
        return total;
    }

    @Override
    public int getSellableStockTotal() {
        int total = 0;
        for (Product p : inventoryService.getAllProducts()) {
            total += inventoryService.getSellableStock(p.getProductId(), java.time.LocalDate.now());
        }
        return total;
    }

    @Override
    public int getLowStockCount() {
        int count = 0;
        for (Product p : inventoryService.getAllProducts()) {
            int currentStock = inventoryService.getSellableStock(p.getProductId(), java.time.LocalDate.now());
            if (currentStock <= p.getLowStockThreshold()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getExpiredBatchCount() {
        return getExpiredBatches().size();
    }

    private List<ProductBatch> getExpiredBatches() {
        List<ProductBatch> result = new ArrayList<>();
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.isExpired()) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public int getNearExpiryCount(int days) {
        return getNearExpiryBatches(days).size();
    }

    private List<ProductBatch> getNearExpiryBatches(int days) {
        List<ProductBatch> result = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate expiryCutoff = today.plusDays(days);
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired() && b.getQuantity() > 0 && !b.getExpiryDate().isBefore(today) && !b.getExpiryDate().isAfter(expiryCutoff)) {
                result.add(b);
            }
        }
        return result;
    }

    @Override
    public BigDecimal getSalesTotalForDay(java.time.LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Sale s : salesService.getAllSales()) {
            if (s.getSaleDateTime() != null && s.getSaleDateTime().toLocalDate().equals(date)) {
                total = total.add(s.getFinalTotal());
            }
        }
        return total;
    }

    @Override
    public List<Sale> getSalesHistory() {
        return salesService.getAllSales();
    }

    @Override
    public List<WastageRecord> getWastageHistory() {
        List<WastageRecord> result = new ArrayList<>();
        try {
            for (WastageRecord r : dataStore.loadWastageRecords()) {
                result.add(r);
            }
        } catch (Exception e) {
            // If storage load fails, return empty list
        }
        return result;
    }

    @Override
    public int getTotalWastageQuantity() {
        int total = 0;
        try {
            for (WastageRecord r : dataStore.loadWastageRecords()) {
                total += r.getQuantity();
            }
        } catch (Exception e) {
            // If storage load fails, return 0
        }
        return total;
    }

    @Override
    public BigDecimal getTotalWastageAmount() {
        BigDecimal total = BigDecimal.ZERO;
        try {
            for (WastageRecord r : dataStore.loadWastageRecords()) {
                // Wastage records don't have unit price in the contract format,
                // so we return zero for the amount and rely on quantity tracking
                // In a full implementation, the wastage might reference the batch's product price
            }
        } catch (Exception e) {
            // If storage load fails, return zero
        }
        return total;
    }
}