package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standard implementation of SalesService.
 * Enforces FEFO (First Expired, First Out) stock allocation and atomic transactional persistence.
 */
public class SalesServiceImpl implements SalesService {

    private final InventoryService inventoryService;
    private final DataStore dataStore;

    private static final DateTimeFormatter ID_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public SalesServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        if (inventoryService == null || dataStore == null) {
            throw new IllegalArgumentException("InventoryService and DataStore cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.dataStore = dataStore;
    }

    @Override
    public Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod) throws SaleException {
        return commitSale(productQuantities, paymentMethod, null);
    }

    @Override
    public synchronized Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod, String notes) throws SaleException {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new SaleException("Cannot checkout: Cart is empty.");
        }

        PaymentMethod pm = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;

        // 1. Pre-validation of quantities and existence
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String prodId = entry.getKey();
            Integer requestedQty = entry.getValue();

            if (requestedQty == null || requestedQty <= 0) {
                throw new SaleException("Invalid quantity (" + requestedQty + ") requested for product ID: " + prodId);
            }

            Product product = inventoryService.getProductById(prodId);
            if (product == null) {
                throw new SaleException("Product not found in catalog: " + prodId);
            }
            if (!product.isActive()) {
                throw new SaleException("Cannot sell inactive product: '" + product.getName() + "' (ID: " + prodId + ").");
            }

            int availableStock = inventoryService.getSellableStock(prodId, today);
            if (requestedQty > availableStock) {
                throw new SaleException("Insufficient sellable stock for '" + product.getName() + "'. " +
                        "Requested: " + requestedQty + ", Available: " + availableStock);
            }
        }

        // 2. Perform FEFO Allocation across batches
        String saleId = "SALE-" + LocalDateTime.now().format(ID_DATE_FMT) + "-" + String.format("%04d", new Random().nextInt(10000));
        Sale sale = new Sale(saleId, LocalDateTime.now(), BigDecimal.ZERO, pm, PaymentStatus.PAID);
        if (notes != null && !notes.trim().isEmpty()) {
            sale.setNotes(notes.trim());
        }

        List<SaleItem> generatedSaleItems = new ArrayList<>();
        Map<String, Integer> batchDeductions = new HashMap<>();

        int itemCounter = 1;
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String prodId = entry.getKey();
            int remainingToAllocate = entry.getValue();
            Product product = inventoryService.getProductById(prodId);

            // Fetch non-expired batches with quantity > 0, sorted by FEFO
            List<ProductBatch> eligibleBatches = new ArrayList<>();
            for (ProductBatch b : inventoryService.getBatchesByProductId(prodId)) {
                if (!b.isExpired(today) && b.getQuantity() > 0 && !b.isRecalled()) {
                    eligibleBatches.add(b);
                }
            }
            eligibleBatches.sort(Comparator.naturalOrder()); // FEFO order

            for (ProductBatch b : eligibleBatches) {
                if (remainingToAllocate <= 0) break;

                int currentBatchQty = b.getQuantity();
                int take = Math.min(currentBatchQty, remainingToAllocate);

                String itemId = "SI-" + saleId + "-" + String.format("%03d", itemCounter++);
                SaleItem item = new SaleItem(
                        itemId,
                        saleId,
                        product.getProductId(),
                        product.getName(),
                        b.getBatchId(),
                        take,
                        product.getSellingPrice() // Actual selling price at sale time
                );
                item.setCostPrice(b.getCostPrice());

                generatedSaleItems.add(item);
                sale.addItem(item);

                batchDeductions.put(b.getBatchId(), take);
                remainingToAllocate -= take;
            }

            if (remainingToAllocate > 0) {
                throw new SaleException("Unexpected stock deficit during FEFO allocation for product: " + product.getName());
            }
        }

        // 3. Atomically persist updated batches, sale, and sale items
        try {
            dataStore.executeTransaction(() -> {
                // Apply staged deductions to persistent batches
                List<ProductBatch> allBatches = dataStore.loadBatches();
                for (ProductBatch b : allBatches) {
                    if (batchDeductions.containsKey(b.getBatchId())) {
                        int deduct = batchDeductions.get(b.getBatchId());
                        b.deductQuantity(deduct);
                    }
                }
                dataStore.saveBatches(allBatches);

                // Save sale header
                List<Sale> allSales = dataStore.loadSales();
                allSales.add(sale);
                dataStore.saveSales(allSales);

                // Save sale items
                List<SaleItem> allItems = dataStore.loadSaleItems();
                allItems.addAll(generatedSaleItems);
                dataStore.saveSaleItems(allItems);
            });
        } catch (Exception e) {
            // Revert memory cache if persistence fails
            inventoryService.reload();
            throw new SaleException("Failed to commit sale transaction: " + e.getMessage(), e);
        }

        // 4. Synchronize inventory in memory
        inventoryService.reload();
        saveReceiptTextFile(sale);
        return sale;
    }

    private void saveReceiptTextFile(Sale sale) {
        try {
            File receiptsDir = new File(dataStore.getDataDirectory(), "receipts");
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs();
            }
            File receiptFile = new File(receiptsDir, sale.getSaleId() + ".txt");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(receiptFile), java.nio.charset.StandardCharsets.UTF_8))) {
                pw.println("==================================================");
                pw.println("                   SHELFLIFE                      ");
                pw.println("      Inventory, Expiry & POS Billing System      ");
                pw.println("==================================================");
                pw.println("Receipt ID:  " + sale.getSaleId());
                pw.println("Date & Time: " + sale.getSaleDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                pw.println("Payment:     " + sale.getPaymentMethod().getDisplayName());
                pw.println("Status:      " + sale.getPaymentStatus().getDisplayName());
                pw.println("--------------------------------------------------");
                pw.printf("%-18s %-10s %5s %12s%n", "Item", "Batch", "Qty", "Price (PKR)");
                pw.println("--------------------------------------------------");
                for (SaleItem it : sale.getItems()) {
                    String name = it.getProductName() != null ? it.getProductName() : it.getProductId();
                    if (name.length() > 18) name = name.substring(0, 15) + "...";
                    pw.printf("%-18s %-10s %5d %12s%n", name, it.getBatchId(), it.getQuantity(), "Rs. " + it.getLineTotal().toPlainString());
                }
                pw.println("==================================================");
                pw.printf("%-35s %12s%n", "GRAND TOTAL (PKR):", "Rs. " + sale.getTotalAmount().toPlainString());
                pw.println("==================================================");
                pw.println("         Thank you for your business!             ");
                pw.println("==================================================");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not auto-save receipt file: " + e.getMessage());
        }
    }

    @Override
    public List<Sale> getAllSales() {
        try {
            List<Sale> sales = dataStore.loadSales();
            List<SaleItem> allItems = dataStore.loadSaleItems();
            Map<String, List<SaleItem>> itemsBySale = new HashMap<>();
            for (SaleItem item : allItems) {
                itemsBySale.computeIfAbsent(item.getSaleId().toUpperCase(), k -> new ArrayList<>()).add(item);
            }
            for (Sale s : sales) {
                List<SaleItem> items = itemsBySale.get(s.getSaleId().toUpperCase());
                if (items != null) {
                    s.setItems(items);
                }
            }
            // Sort by saleDateTime descending (most recent first)
            sales.sort((a, b) -> b.getSaleDateTime().compareTo(a.getSaleDateTime()));
            return sales;
        } catch (Exception e) {
            System.err.println("Warning: Failed to load sales: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Sale getSaleById(String saleId) {
        if (saleId == null) return null;
        for (Sale s : getAllSales()) {
            if (s.getSaleId().equalsIgnoreCase(saleId.trim())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public List<SaleItem> getSaleItems(String saleId) {
        List<SaleItem> result = new ArrayList<>();
        if (saleId == null) return result;
        try {
            for (SaleItem item : dataStore.loadSaleItems()) {
                if (item.getSaleId().equalsIgnoreCase(saleId.trim())) {
                    result.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load sale items: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Sale commitSaleWithPayments(Map<String, Integer> productQuantities, List<PaymentLine> payments,
                                        String customerId, BigDecimal discountAmount, String notes,
                                        String cashierUsername) throws SaleException {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new SaleException("Cart cannot be empty. Please add items before checking out.");
        }

        LocalDate today = LocalDate.now();

        // 1. Pre-validation of catalog, active state, and available stock
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String prodId = entry.getKey();
            Integer requestedQty = entry.getValue();

            if (requestedQty == null || requestedQty <= 0) {
                throw new SaleException("Invalid quantity (" + requestedQty + ") requested for product ID: " + prodId);
            }

            Product product = inventoryService.getProductById(prodId);
            if (product == null) {
                throw new SaleException("Product not found in catalog: " + prodId);
            }
            if (!product.isActive()) {
                throw new SaleException("Cannot sell inactive product: '" + product.getName() + "' (ID: " + prodId + ").");
            }

            int availableStock = 0;
            for (ProductBatch b : inventoryService.getBatchesByProductId(prodId)) {
                if (!b.isExpired(today) && b.getQuantity() > 0 && !b.isRecalled()) {
                    availableStock += b.getQuantity();
                }
            }
            if (requestedQty > availableStock) {
                throw new SaleException("Insufficient sellable stock for '" + product.getName() + "'. " +
                        "Requested: " + requestedQty + ", Available: " + availableStock);
            }
        }

        // 2. Perform FEFO Allocation across non-recalled batches
        String saleId = "SALE-" + LocalDateTime.now().format(ID_DATE_FMT) + "-" + String.format("%04d", new Random().nextInt(10000));
        PaymentMethod primaryMethod = (payments != null && !payments.isEmpty()) ? payments.get(0).getPaymentMethod() : PaymentMethod.CASH;
        Sale sale = new Sale(saleId, LocalDateTime.now(), BigDecimal.ZERO, primaryMethod, PaymentStatus.PAID);
        sale.setCashierUsername(cashierUsername != null ? cashierUsername : "cashier");
        sale.setCustomerId(customerId);
        if (notes != null && !notes.trim().isEmpty()) {
            sale.setNotes(notes.trim());
        }

        List<SaleItem> generatedSaleItems = new ArrayList<>();
        Map<String, Integer> batchDeductions = new HashMap<>();

        int itemCounter = 1;
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String prodId = entry.getKey();
            int remainingToAllocate = entry.getValue();
            Product product = inventoryService.getProductById(prodId);

            List<ProductBatch> eligibleBatches = new ArrayList<>();
            for (ProductBatch b : inventoryService.getBatchesByProductId(prodId)) {
                if (!b.isExpired(today) && b.getQuantity() > 0 && !b.isRecalled()) {
                    eligibleBatches.add(b);
                }
            }
            eligibleBatches.sort(Comparator.naturalOrder()); // FEFO order

            for (ProductBatch b : eligibleBatches) {
                if (remainingToAllocate <= 0) break;

                int currentBatchQty = b.getQuantity();
                int take = Math.min(currentBatchQty, remainingToAllocate);

                String itemId = "SI-" + saleId + "-" + String.format("%03d", itemCounter++);
                SaleItem item = new SaleItem(
                        itemId,
                        saleId,
                        product.getProductId(),
                        product.getName(),
                        b.getBatchId(),
                        take,
                        product.getSellingPrice()
                );
                item.setCostPrice(b.getCostPrice());

                generatedSaleItems.add(item);
                sale.addItem(item);
                calculatedSubtotal = calculatedSubtotal.add(item.getLineTotal());

                batchDeductions.put(b.getBatchId(), take);
                remainingToAllocate -= take;
            }

            if (remainingToAllocate > 0) {
                throw new SaleException("Unexpected stock deficit during FEFO allocation for product: " + product.getName());
            }
        }

        // Apply discount & tax
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        sale.setDiscountAmount(disc);
        sale.setSubtotal(calculatedSubtotal);

        BigDecimal taxableAmount = calculatedSubtotal.subtract(disc);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;

        BigDecimal taxAmount = BigDecimal.ZERO;
        try {
            AppSettings settings = dataStore.loadSettings();
            if (settings != null && settings.isTaxEnabled() && settings.getTaxRatePercent().compareTo(BigDecimal.ZERO) > 0) {
                taxAmount = taxableAmount.multiply(settings.getTaxRatePercent())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            // fallback
        }
        sale.setTax(taxAmount);
        sale.setTotalAmount(taxableAmount.add(taxAmount));

        // Prepare payment lines with generated saleId
        List<PaymentLine> finalPaymentLines = new ArrayList<>();
        if (payments != null) {
            int pCount = 1;
            for (PaymentLine pl : payments) {
                pl.setSaleId(saleId);
                pl.setPaymentLineId(saleId + "-PAY" + (pCount++));
                pl.setTimestamp(LocalDateTime.now());
                finalPaymentLines.add(pl);
            }
        }
        sale.setPaymentLines(finalPaymentLines);

        // 3. Atomically persist
        try {
            dataStore.executeTransaction(() -> {
                List<ProductBatch> allBatches = dataStore.loadBatches();
                for (ProductBatch b : allBatches) {
                    if (batchDeductions.containsKey(b.getBatchId())) {
                        int deduct = batchDeductions.get(b.getBatchId());
                        b.deductQuantity(deduct);
                    }
                }
                dataStore.saveBatches(allBatches);

                List<Sale> allSales = dataStore.loadSales();
                allSales.add(sale);
                dataStore.saveSales(allSales);

                List<SaleItem> allItems = dataStore.loadSaleItems();
                allItems.addAll(generatedSaleItems);
                dataStore.saveSaleItems(allItems);

                if (!finalPaymentLines.isEmpty()) {
                    List<PaymentLine> allPayments = dataStore.loadPaymentLines();
                    allPayments.addAll(finalPaymentLines);
                    dataStore.savePaymentLines(allPayments);
                }
            });
        } catch (Exception e) {
            inventoryService.reload();
            throw new SaleException("Failed to commit sale transaction: " + e.getMessage(), e);
        }

        inventoryService.reload();
        saveReceiptTextFile(sale);
        return sale;
    }

    @Override
    public List<String> getTopSellingProductIds(int limit, int days) {
        List<String> result = new ArrayList<>();
        try {
            LocalDate cutoff = LocalDate.now().minusDays(days > 0 ? days : 30);
            List<Sale> allSales = getAllSales();
            Set<String> recentSaleIds = new HashSet<>();
            for (Sale s : allSales) {
                if (s.getSaleDateTime().toLocalDate().isAfter(cutoff) || s.getSaleDateTime().toLocalDate().isEqual(cutoff)) {
                    recentSaleIds.add(s.getSaleId());
                }
            }

            Map<String, Integer> productTotals = new HashMap<>();
            for (SaleItem item : dataStore.loadSaleItems()) {
                if (recentSaleIds.contains(item.getSaleId())) {
                    productTotals.put(item.getProductId(), productTotals.getOrDefault(item.getProductId(), 0) + item.getQuantity());
                }
            }

            // If sales history is low, include all sales
            if (productTotals.isEmpty()) {
                for (SaleItem item : dataStore.loadSaleItems()) {
                    productTotals.put(item.getProductId(), productTotals.getOrDefault(item.getProductId(), 0) + item.getQuantity());
                }
            }

            // Sort products by total quantity sold descending
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productTotals.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> e : sorted) {
                if (result.size() >= limit) break;
                result.add(e.getKey());
            }

            // If still less than limit, pad with active products from inventory
            if (result.size() < limit) {
                for (Product p : inventoryService.getAllProducts()) {
                    if (p.isActive() && !result.contains(p.getProductId())) {
                        result.add(p.getProductId());
                        if (result.size() >= limit) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not fetch top selling products: " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean isBelowCost(String productId, BigDecimal sellingPrice, BigDecimal discount) {
        if (productId == null || sellingPrice == null) return false;
        BigDecimal effectivePrice = sellingPrice.subtract(discount != null ? discount : BigDecimal.ZERO);
        LocalDate today = LocalDate.now();

        List<ProductBatch> batches = new ArrayList<>();
        for (ProductBatch b : inventoryService.getBatchesByProductId(productId)) {
            if (!b.isExpired(today) && b.getQuantity() > 0 && !b.isRecalled()) {
                batches.add(b);
            }
        }
        if (batches.isEmpty()) return false;
        batches.sort(Comparator.naturalOrder());
        ProductBatch nextBatch = batches.get(0);
        return effectivePrice.compareTo(nextBatch.getCostPrice()) < 0;
    }

    @Override
    public void markDuplicateReceipt(String saleId) throws Exception {
        if (saleId == null) return;
        dataStore.executeTransaction(() -> {
            List<Sale> allSales = dataStore.loadSales();
            for (Sale s : allSales) {
                if (s.getSaleId().equalsIgnoreCase(saleId.trim())) {
                    s.incrementReprintCount();
                    break;
                }
            }
            dataStore.saveSales(allSales);
        });
    }

    @Override
    public void logPriceOverride(PriceOverrideLog override) throws Exception {
        if (override == null) return;
        dataStore.executeTransaction(() -> {
            List<PriceOverrideLog> list = dataStore.loadPriceOverrides();
            list.add(override);
            dataStore.savePriceOverrides(list);
        });
    }

    @Override
    public List<PaymentLine> getPaymentLines(String saleId) {
        List<PaymentLine> list = new ArrayList<>();
        if (saleId == null) return list;
        try {
            for (PaymentLine pl : dataStore.loadPaymentLines()) {
                if (pl.getSaleId().equalsIgnoreCase(saleId.trim())) {
                    list.add(pl);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load payment lines: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void reload() {
        inventoryService.reload();
    }
}
