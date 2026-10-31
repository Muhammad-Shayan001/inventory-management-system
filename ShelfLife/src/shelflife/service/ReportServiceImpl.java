package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Standard implementation of ReportService.
 * Dynamically computes analytics from underlying services and data store.
 */
public class ReportServiceImpl implements ReportService {

    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final DataStore dataStore;

    public ReportServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        this(inventoryService, null, dataStore);
    }

    public ReportServiceImpl(InventoryService inventoryService, SalesService salesService, DataStore dataStore) {
        if (inventoryService == null || dataStore == null) {
            throw new IllegalArgumentException("InventoryService and DataStore cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.salesService = salesService;
        this.dataStore = dataStore;
    }

    @Override
    public void refreshData() {
        inventoryService.reload();
        if (salesService != null) {
            salesService.reload();
        }
    }

    @Override
    public int getTotalProductCount() {
        return inventoryService.getAllProducts().size();
    }

    @Override
    public int getTotalPhysicalStock() {
        int total = 0;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            total += b.getQuantity();
        }
        return total;
    }

    @Override
    public int getTotalSellableStock() {
        int total = 0;
        LocalDate today = LocalDate.now();
        for (Product p : inventoryService.getAllProducts()) {
            total += inventoryService.getSellableStock(p.getProductId(), today);
        }
        return total;
    }

    @Override
    public int getLowStockCount() {
        int count = 0;
        for (Product p : inventoryService.getAllProducts()) {
            if (inventoryService.isLowStock(p.getProductId())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getExpiredBatchCount() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.isExpired(today)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getNearExpiryBatchCount(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(daysThreshold);
        int count = 0;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired(today) && b.getQuantity() > 0) {
                LocalDate exp = b.getExpiryDate();
                if (!exp.isBefore(today) && !exp.isAfter(cutoff)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public BigDecimal getSalesTotalForDate(LocalDate date) {
        if (date == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Sale s : getSalesHistory()) {
            if (s.getSaleDateTime().toLocalDate().equals(date) && s.getPaymentStatus() == PaymentStatus.PAID) {
                total = total.add(s.getTotalAmount());
            }
        }
        return total;
    }

    @Override
    public BigDecimal getTotalRevenue() {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Sale s : getSalesHistory()) {
            if (s.getPaymentStatus() == PaymentStatus.PAID) {
                total = total.add(s.getTotalAmount());
            }
        }
        return total;
    }

    @Override
    public List<Sale> getSalesHistory() {
        if (salesService != null) {
            return salesService.getAllSales();
        }
        try {
            List<Sale> sales = dataStore.loadSales();
            sales.sort((a, b) -> b.getSaleDateTime().compareTo(a.getSaleDateTime()));
            return sales;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProductBatch> getStockInformation() {
        return inventoryService.getAllBatches();
    }

    @Override
    public List<WastageRecord> getWastageHistory() {
        try {
            List<WastageRecord> list = dataStore.loadWastageRecords();
            list.sort((a, b) -> b.getWastageDateTime().compareTo(a.getWastageDateTime()));
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getTotalWastageQuantity() {
        int total = 0;
        for (WastageRecord r : getWastageHistory()) {
            total += r.getQuantity();
        }
        return total;
    }

    @Override
    public BigDecimal getTotalWastageCost() {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (WastageRecord r : getWastageHistory()) {
            ProductBatch b = inventoryService.getBatchById(r.getBatchId());
            if (b != null) {
                Product p = inventoryService.getProductById(b.getProductId());
                if (p != null) {
                    BigDecimal cost = p.getSellingPrice().multiply(BigDecimal.valueOf(r.getQuantity()));
                    total = total.add(cost);
                }
            }
        }
        return total;
    }

    @Override
    public BigDecimal getTotalRefunds() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (SaleReturn r : getReturnHistory()) {
            if (r.getTotalRefund() != null) {
                sum = sum.add(r.getTotalRefund());
            }
        }
        return sum;
    }

    @Override
    public List<SaleReturn> getReturnHistory() {
        try {
            List<SaleReturn> list = dataStore.loadReturns();
            list.sort((a, b) -> b.getReturnDate().compareTo(a.getReturnDate()));
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Product> getReorderList() {
        List<Product> reorder = new ArrayList<>();
        for (Product p : inventoryService.getAllProducts()) {
            if (p.isActive() && inventoryService.isLowStock(p.getProductId())) {
                reorder.add(p);
            }
        }
        return reorder;
    }

    @Override
    public BigDecimal getTotalProfit() {
        try {
            BigDecimal totalRevenue = getTotalRevenue();
            BigDecimal totalCogs = BigDecimal.ZERO;
            List<SaleItem> items = dataStore.loadSaleItems();
            for (SaleItem it : items) {
                BigDecimal cost = (it.getCostPrice() != null && it.getCostPrice().compareTo(BigDecimal.ZERO) > 0) ? 
                        it.getCostPrice() : BigDecimal.ZERO;
                totalCogs = totalCogs.add(cost.multiply(BigDecimal.valueOf(it.getQuantity())));
            }

            BigDecimal grossProfit = totalRevenue.subtract(totalCogs);
            BigDecimal wastage = getTotalWastageCost();
            BigDecimal refunds = getTotalRefunds();

            BigDecimal netProfit = grossProfit.subtract(wastage).subtract(refunds);
            return netProfit.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public Product getProductById(String productId) {
        return inventoryService.getProductById(productId);
    }
}
