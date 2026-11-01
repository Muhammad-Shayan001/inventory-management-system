package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
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

    // =====================================================================
    // Analytics for the Business Overview dashboard
    // =====================================================================

    /** Paid sales whose date falls in [from, to]. */
    private List<Sale> paidSalesBetween(LocalDate from, LocalDate to) {
        List<Sale> result = new ArrayList<>();
        for (Sale s : getSalesHistory()) {
            LocalDate d = s.getSaleDateTime().toLocalDate();
            if (s.getPaymentStatus() == PaymentStatus.PAID && !d.isBefore(from) && !d.isAfter(to)) {
                result.add(s);
            }
        }
        return result;
    }

    /** Sale items grouped by sale id, loaded once per call. */
    private Map<String, List<SaleItem>> itemsBySale() {
        Map<String, List<SaleItem>> map = new HashMap<>();
        try {
            for (SaleItem it : dataStore.loadSaleItems()) {
                List<SaleItem> list = map.get(it.getSaleId());
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(it.getSaleId(), list);
                }
                list.add(it);
            }
        } catch (Exception e) {
            // no items available: profit figures fall back to zero cost
        }
        return map;
    }

    private static BigDecimal costOf(List<SaleItem> items) {
        BigDecimal cost = BigDecimal.ZERO;
        if (items == null) {
            return cost;
        }
        for (SaleItem it : items) {
            if (it.getCostPrice() != null && it.getCostPrice().signum() > 0) {
                cost = cost.add(it.getCostPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }
        return cost;
    }

    /** Sale items on disk do not store the name, so look it up in the catalogue. */
    private String displayName(String productId, String fallback) {
        Product p = inventoryService.getProductById(productId);
        return p != null ? p.getName() : (fallback != null ? fallback : productId);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<LocalDate, BigDecimal> getDailySales(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> days = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.put(d, money(BigDecimal.ZERO));
        }
        for (Sale s : paidSalesBetween(from, to)) {
            LocalDate d = s.getSaleDateTime().toLocalDate();
            days.put(d, days.get(d).add(s.getTotalAmount()));
        }
        return days;
    }

    @Override
    public BigDecimal getRevenueForRange(LocalDate from, LocalDate to) {
        BigDecimal total = BigDecimal.ZERO;
        for (Sale s : paidSalesBetween(from, to)) {
            total = total.add(s.getTotalAmount());
        }
        return money(total);
    }

    @Override
    public BigDecimal getGrossProfitForRange(LocalDate from, LocalDate to) {
        Map<String, List<SaleItem>> items = itemsBySale();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (Sale s : paidSalesBetween(from, to)) {
            revenue = revenue.add(s.getTotalAmount());
            cost = cost.add(costOf(items.get(s.getSaleId())));
        }
        return money(revenue.subtract(cost));
    }

    @Override
    public BigDecimal getAverageSaleValue(LocalDate from, LocalDate to) {
        List<Sale> sales = paidSalesBetween(from, to);
        if (sales.isEmpty()) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Sale s : sales) {
            total = total.add(s.getTotalAmount());
        }
        return total.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public List<ProductSales> getTopProducts(LocalDate from, LocalDate to, int limit) {
        Map<String, List<SaleItem>> items = itemsBySale();
        Map<String, int[]> qty = new HashMap<>();
        Map<String, BigDecimal[]> money = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (Sale s : paidSalesBetween(from, to)) {
            List<SaleItem> list = items.get(s.getSaleId());
            if (list == null) {
                continue;
            }
            for (SaleItem it : list) {
                String id = it.getProductId();
                int[] q = qty.get(id);
                BigDecimal[] m = money.get(id);
                if (q == null) {
                    q = new int[1];
                    m = new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO};
                    qty.put(id, q);
                    money.put(id, m);
                    names.put(id, displayName(id, it.getProductName()));
                }
                q[0] += it.getQuantity();
                m[0] = m[0].add(it.getLineTotal());
                BigDecimal unitCost = it.getCostPrice() == null ? BigDecimal.ZERO : it.getCostPrice();
                m[1] = m[1].add(it.getLineTotal().subtract(unitCost.multiply(BigDecimal.valueOf(it.getQuantity()))));
            }
        }
        List<ProductSales> result = new ArrayList<>();
        for (String id : qty.keySet()) {
            result.add(new ProductSales(id, names.get(id), qty.get(id)[0], money(money.get(id)[0]), money(money.get(id)[1])));
        }
        result.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));
        return result.size() > limit ? new ArrayList<>(result.subList(0, Math.max(0, limit))) : result;
    }

    @Override
    public Map<String, BigDecimal> getSalesByPaymentMethod(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Sale s : paidSalesBetween(from, to)) {
            if (s.getPaymentLines() != null && !s.getPaymentLines().isEmpty()) {
                for (PaymentLine line : s.getPaymentLines()) {
                    addTo(totals, line.getPaymentMethod().getDisplayName(), line.getAmount());
                }
            } else if (s.getPaymentMethod() != null) {
                addTo(totals, s.getPaymentMethod().getDisplayName(), s.getTotalAmount());
            }
        }
        return totals;
    }

    private static void addTo(Map<String, BigDecimal> map, String key, BigDecimal amount) {
        BigDecimal current = map.get(key);
        map.put(key, (current == null ? BigDecimal.ZERO : current).add(amount == null ? BigDecimal.ZERO : amount));
    }

    @Override
    public Map<String, BigDecimal> getWastageByReason(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (WastageRecord r : getWastageHistory()) {
            LocalDate d = r.getWastageDateTime().toLocalDate();
            if (d.isBefore(from) || d.isAfter(to)) {
                continue;
            }
            String reason = r.getReason() == null || r.getReason().trim().isEmpty() ? "Unspecified" : r.getReason().trim();
            addTo(totals, reason, BigDecimal.valueOf(r.getQuantity()));
        }
        return totals;
    }

    @Override
    public Map<Integer, BigDecimal> getSalesByHour(LocalDate from, LocalDate to) {
        Map<Integer, BigDecimal> hours = new TreeMap<>();
        for (Sale s : paidSalesBetween(from, to)) {
            int h = s.getSaleDateTime().getHour();
            BigDecimal current = hours.get(h);
            hours.put(h, (current == null ? BigDecimal.ZERO : current).add(s.getTotalAmount()));
        }
        return hours;
    }

    @Override
    public List<MonthlyRow> getMonthlySalesAndProfit(int months) {
        int count = Math.max(1, months);
        YearMonth current = YearMonth.now();
        Map<String, List<SaleItem>> items = itemsBySale();
        List<MonthlyRow> rows = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;
            for (Sale s : paidSalesBetween(ym.atDay(1), ym.atEndOfMonth())) {
                revenue = revenue.add(s.getTotalAmount());
                cost = cost.add(costOf(items.get(s.getSaleId())));
            }
            rows.add(new MonthlyRow(ym, money(revenue), money(revenue.subtract(cost))));
        }
        return rows;
    }

    @Override
    public StockHealth getStockHealth(LocalDate asOf, int nearExpiryDays) {
        LocalDate nearLimit = asOf.plusDays(nearExpiryDays);
        int good = 0;
        int near = 0;
        int expired = 0;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getQuantity() <= 0) {
                continue;
            }
            if (b.isRecalled() || b.isExpired(asOf)) {
                expired += b.getQuantity();
            } else if (!b.getExpiryDate().isAfter(nearLimit)) {
                near += b.getQuantity();
            } else {
                good += b.getQuantity();
            }
        }
        return new StockHealth(good, near, expired);
    }

    @Override
    public BigDecimal getStockValueAtCost() {
        BigDecimal total = BigDecimal.ZERO;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getQuantity() > 0 && b.getCostPrice() != null) {
                total = total.add(b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
            }
        }
        return money(total);
    }

    @Override
    public List<Product> getDeadStock(int days) {
        LocalDate today = LocalDate.now();
        Map<String, List<SaleItem>> items = itemsBySale();
        Set<String> sold = new HashSet<>();
        for (Sale s : paidSalesBetween(today.minusDays(days), today)) {
            List<SaleItem> list = items.get(s.getSaleId());
            if (list != null) {
                for (SaleItem it : list) {
                    sold.add(it.getProductId());
                }
            }
        }
        List<Product> dead = new ArrayList<>();
        for (Product p : inventoryService.getAllProducts()) {
            if (p.isActive() && !sold.contains(p.getProductId()) && inventoryService.getTotalStock(p.getProductId()) > 0) {
                dead.add(p);
            }
        }
        return dead;
    }
}
