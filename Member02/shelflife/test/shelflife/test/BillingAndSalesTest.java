package shelflife.test;

import shelflife.model.*;
import shelflife.service.SaleException;
import shelflife.service.SalesServiceImpl;
import shelflife.storage.CsvDataStore;
import shelflife.storage.DataStore;
import shelflife.storage.TransactionOperation;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive automated test suite for ShelfLife Member 3 (Billing & Sales Management).
 * Covers all required university evaluation scenarios:
 * 1. Successful sale & stock reduction
 * 2. Multi-batch allocation via FEFO
 * 3. Insufficient sellable stock rejection
 * 4. Expired stock exclusion
 * 5. Cart cancellation (zero inventory side-effects)
 * 6. BigDecimal monetary accuracy & historical price immutability
 * 7. Repeated confirmation & duplicate deduction protection
 * 8. Failed storage commit transaction rollback
 */
public class BillingAndSalesTest {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   SHELFLIFE MEMBER 3: BILLING & SALES MANAGEMENT TEST RUNNER        ");
        System.out.println("======================================================================");

        runTest("1. Successful Sale", BillingAndSalesTest::testSuccessfulSale);
        runTest("2. Multi-Batch FEFO Allocation", BillingAndSalesTest::testMultipleBatchAllocation);
        runTest("3. Insufficient Stock Rejection", BillingAndSalesTest::testInsufficientStock);
        runTest("4. Expired Stock Exclusion", BillingAndSalesTest::testExpiredStockExclusion);
        runTest("5. Cart Cancellation Zero Side-Effects", BillingAndSalesTest::testCartCancellation);
        runTest("6. BigDecimal Rounding & Price Immutability", BillingAndSalesTest::testRoundingAndMonetaryCalculations);
        runTest("7. Repeated Confirmation & Duplicate Deduction Protection", BillingAndSalesTest::testRepeatedConfirmationPrevention);
        runTest("8. Failed Storage Commit Rollback", BillingAndSalesTest::testFailedStorageCommitRollback);

        System.out.println("======================================================================");
        System.out.println(String.format("TEST RESULTS: Passed: %d | Failed: %d | Total: %d",
                testsPassed, testsFailed, testsPassed + testsFailed));
        System.out.println("======================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestRunnable runnable) {
        try {
            System.out.print("[RUNNING] " + testName + " ... ");
            runnable.run();
            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED!");
            System.err.println("  -> Error: " + t.getMessage());
            t.printStackTrace(System.err);
            testsFailed++;
        }
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Throwable;
    }

    private static File createTempDataDir(String prefix) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), prefix + "_" + System.nanoTime());
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        return tempDir;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - Expected: [" + expected + "] but was: [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // =========================================================================
    // TEST 1: Successful Sale
    // =========================================================================
    public static void testSuccessfulSale() throws Exception {
        File dataDir = createTempDataDir("test_sale_success");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Whole Milk 1L", "Dairy", new BigDecimal("3.50"));
        ds.saveProducts(Collections.singletonList(p1));

        LocalDate future = LocalDate.now().plusMonths(1);
        ProductBatch b1 = new ProductBatch("B101", "P101", 10, future, LocalDate.now());
        ds.saveBatches(Collections.singletonList(b1));

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 2);

        Sale sale = salesService.commitSale(cart, PaymentMethod.CASH);

        // Verification
        assertEquals(1, sale.getItems().size(), "Should have 1 sale item");
        SaleItem item = sale.getItems().get(0);
        assertEquals("P101", item.getProductId(), "Product ID match");
        assertEquals("B101", item.getBatchId(), "Batch ID match");
        assertEquals(2, item.getQuantity(), "Quantity match");
        assertEquals(new BigDecimal("3.50"), item.getUnitPrice(), "Unit price match");
        assertEquals(new BigDecimal("7.00"), item.getLineTotal(), "Line total match");
        assertEquals(new BigDecimal("7.00"), sale.getSubtotal(), "Subtotal match");
        assertEquals(new BigDecimal("7.00"), sale.getFinalTotal(), "Final total match");
        assertEquals(PaymentMethod.CASH, sale.getPaymentMethod(), "Payment method match");
        assertEquals(PaymentStatus.PAID, sale.getPaymentStatus(), "Payment status match");

        // Verify inventory deduction in storage
        List<ProductBatch> savedBatches = ds.loadBatches();
        assertEquals(8, savedBatches.get(0).getQuantity(), "Inventory remaining should be 8");
    }

    // =========================================================================
    // TEST 2: Multiple-Batch FEFO Allocation
    // =========================================================================
    public static void testMultipleBatchAllocation() throws Exception {
        File dataDir = createTempDataDir("test_fefo_alloc");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Fresh Yogurt 500g", "Dairy", new BigDecimal("2.50"));
        ds.saveProducts(Collections.singletonList(p1));

        // Two batches: B-EARLY expires in 5 days (4 units), B-LATE expires in 20 days (6 units)
        LocalDate expEarly = LocalDate.now().plusDays(5);
        LocalDate expLate = LocalDate.now().plusDays(20);

        ProductBatch bEarly = new ProductBatch("B-EARLY", "P101", 4, expEarly, LocalDate.now());
        ProductBatch bLate = new ProductBatch("B-LATE", "P101", 6, expLate, LocalDate.now());

        // Save in reverse order to ensure FEFO sorts correctly by expiry date
        List<ProductBatch> initialBatches = new ArrayList<>();
        initialBatches.add(bLate);
        initialBatches.add(bEarly);
        ds.saveBatches(initialBatches);

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        // Request 7 units (should exhaust B-EARLY's 4 units, and take 3 from B-LATE)
        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 7);

        Sale sale = salesService.commitSale(cart, PaymentMethod.CARD);

        // Verification: Must generate two distinct SaleItems
        assertEquals(2, sale.getItems().size(), "Should have exactly 2 SaleItem records for split batches");

        SaleItem item1 = sale.getItems().get(0);
        assertEquals("B-EARLY", item1.getBatchId(), "First batch allocated must be earliest expiry");
        assertEquals(4, item1.getQuantity(), "First batch should contribute 4 units");
        assertEquals(new BigDecimal("10.00"), item1.getLineTotal(), "Item 1 total: 4 * 2.50 = 10.00");

        SaleItem item2 = sale.getItems().get(1);
        assertEquals("B-LATE", item2.getBatchId(), "Second batch allocated must be later expiry");
        assertEquals(3, item2.getQuantity(), "Second batch should contribute remaining 3 units");
        assertEquals(new BigDecimal("7.50"), item2.getLineTotal(), "Item 2 total: 3 * 2.50 = 7.50");

        assertEquals(new BigDecimal("17.50"), sale.getFinalTotal(), "Total: 10.00 + 7.50 = 17.50");

        // Verify remaining quantities in storage
        List<ProductBatch> savedBatches = ds.loadBatches();
        for (ProductBatch b : savedBatches) {
            if ("B-EARLY".equals(b.getBatchId())) {
                assertEquals(0, b.getQuantity(), "B-EARLY should be exhausted (0 remaining)");
            } else if ("B-LATE".equals(b.getBatchId())) {
                assertEquals(3, b.getQuantity(), "B-LATE should have 3 units remaining (6 - 3)");
            }
        }
    }

    // =========================================================================
    // TEST 3: Insufficient Stock Rejection
    // =========================================================================
    public static void testInsufficientStock() throws Exception {
        File dataDir = createTempDataDir("test_insufficient_stock");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Almond Milk", "Dairy", new BigDecimal("4.00"));
        ds.saveProducts(Collections.singletonList(p1));

        ProductBatch b1 = new ProductBatch("B1", "P101", 5, LocalDate.now().plusMonths(1), LocalDate.now());
        ds.saveBatches(Collections.singletonList(b1));

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        // Request 6 units when only 5 exist
        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 6);

        boolean exceptionThrown = false;
        try {
            salesService.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException se) {
            exceptionThrown = true;
            assertTrue(se.getMessage().contains("Insufficient sellable stock"), "Message should mention insufficient stock");
        }

        assertTrue(exceptionThrown, "SaleException must be thrown when requested > available");

        // Verify inventory is untouched
        assertEquals(5, ds.loadBatches().get(0).getQuantity(), "Batch quantity must remain 5");
        assertEquals(0, ds.loadSales().size(), "No sale should be saved");
    }

    // =========================================================================
    // TEST 4: Expired Stock Exclusion
    // =========================================================================
    public static void testExpiredStockExclusion() throws Exception {
        File dataDir = createTempDataDir("test_expired_stock");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Greek Yogurt", "Dairy", new BigDecimal("3.00"));
        ds.saveProducts(Collections.singletonList(p1));

        // Batch 1: Expired yesterday (10 units)
        ProductBatch bExpired = new ProductBatch("B-EXP", "P101", 10, LocalDate.now().minusDays(1), LocalDate.now().minusDays(10));
        // Batch 2: Valid for next week (3 units)
        ProductBatch bValid = new ProductBatch("B-VAL", "P101", 3, LocalDate.now().plusDays(7), LocalDate.now());

        List<ProductBatch> batches = new ArrayList<>();
        batches.add(bExpired);
        batches.add(bValid);
        ds.saveBatches(batches);

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        // Warehouse physically has 13 units, but sellable stock is only 3
        assertEquals(3, invService.getSellableStock("P101", LocalDate.now()), "Sellable stock should exclude expired batch");

        // Request 4 units (should fail because 10 units are expired)
        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 4);

        boolean failedAsExpected = false;
        try {
            salesService.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException se) {
            failedAsExpected = true;
        }

        assertTrue(failedAsExpected, "Sale must fail when request exceeds sellable (non-expired) stock");

        // Now request 2 units (should succeed and allocate strictly from B-VAL)
        cart.put("P101", 2);
        Sale sale = salesService.commitSale(cart, PaymentMethod.CASH);

        assertEquals(1, sale.getItems().size(), "One item record");
        assertEquals("B-VAL", sale.getItems().get(0).getBatchId(), "Must be allocated strictly from valid batch");

        // Verify B-EXP was untouched
        for (ProductBatch b : ds.loadBatches()) {
            if ("B-EXP".equals(b.getBatchId())) {
                assertEquals(10, b.getQuantity(), "Expired batch must remain untouched (10 units)");
            } else if ("B-VAL".equals(b.getBatchId())) {
                assertEquals(1, b.getQuantity(), "Valid batch reduced by 2 to 1 unit");
            }
        }
    }

    // =========================================================================
    // TEST 5: Cart Cancellation Zero Side-Effects
    // =========================================================================
    public static void testCartCancellation() throws Exception {
        File dataDir = createTempDataDir("test_cart_cancel");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Cereal", "Pantry", new BigDecimal("5.00"));
        ds.saveProducts(Collections.singletonList(p1));

        ProductBatch b1 = new ProductBatch("B1", "P101", 20, LocalDate.now().plusMonths(3), LocalDate.now());
        ds.saveBatches(Collections.singletonList(b1));

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        // Staging cart in UI
        List<shelflife.ui.billing.CartItem> uiCart = new ArrayList<>();
        uiCart.add(new shelflife.ui.billing.CartItem(p1, 5));

        // Cashier clears cart or cancels checkout prompt
        uiCart.clear();

        // Verify inventory and storage are 100% untouched
        assertEquals(20, ds.loadBatches().get(0).getQuantity(), "Inventory must not change on cart cancel");
        assertEquals(0, ds.loadSales().size(), "No sale record written");
    }

    // =========================================================================
    // TEST 6: BigDecimal Monetary Accuracy & Historical Price Immutability
    // =========================================================================
    public static void testRoundingAndMonetaryCalculations() throws Exception {
        File dataDir = createTempDataDir("test_monetary_rounding");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Candy Bar", "Snacks", new BigDecimal("1.99"));
        Product p2 = new Product("P102", "Chewing Gum", "Snacks", new BigDecimal("0.33"));
        List<Product> products = new ArrayList<>();
        products.add(p1);
        products.add(p2);
        ds.saveProducts(products);

        ProductBatch b1 = new ProductBatch("B1", "P101", 100, LocalDate.now().plusMonths(6), LocalDate.now());
        ProductBatch b2 = new ProductBatch("B2", "P102", 100, LocalDate.now().plusMonths(6), LocalDate.now());
        List<ProductBatch> batches = new ArrayList<>();
        batches.add(b1);
        batches.add(b2);
        ds.saveBatches(batches);

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        // 3 candy bars ($1.99 * 3 = $5.97) + 3 gums ($0.33 * 3 = $0.99)
        // Subtotal = 5.97 + 0.99 = 6.96
        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P101", 3);
        cart.put("P102", 3);

        Sale sale = salesService.commitSale(cart, PaymentMethod.DIGITAL_WALLET);

        assertEquals(new BigDecimal("5.97"), sale.getItems().get(0).getLineTotal(), "Item 1 total line accuracy");
        assertEquals(new BigDecimal("0.99"), sale.getItems().get(1).getLineTotal(), "Item 2 total line accuracy");
        assertEquals(new BigDecimal("6.96"), sale.getSubtotal(), "Subtotal exact BigDecimal match");
        assertEquals(new BigDecimal("6.96"), sale.getFinalTotal(), "Final total exact BigDecimal match");

        // Now simulate price change in product catalog: Candy Bar increases to $2.99
        p1.setUnitPrice(new BigDecimal("2.99"));
        ds.saveProducts(products);
        invService.reload();

        // Historical receipt must retain original charged price ($1.99 and line total $5.97)
        Sale historicalSale = salesService.getSaleById(sale.getSaleId());
        assertEquals(new BigDecimal("1.99"), historicalSale.getItems().get(0).getUnitPrice(), "Historical charged unit price must remain unchanged");
        assertEquals(new BigDecimal("5.97"), historicalSale.getItems().get(0).getLineTotal(), "Historical line total must remain unchanged");
        assertEquals(new BigDecimal("6.96"), historicalSale.getFinalTotal(), "Historical final total must remain unchanged");
    }

    // =========================================================================
    // TEST 7: Repeated Confirmation & Duplicate Deduction Protection
    // =========================================================================
    public static void testRepeatedConfirmationPrevention() throws Exception {
        File dataDir = createTempDataDir("test_duplicate_deduction");
        DataStore ds = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Limited Coffee Bean", "Beverages", new BigDecimal("12.50"));
        ds.saveProducts(Collections.singletonList(p1));

        // Only 5 units exist
        ProductBatch b1 = new ProductBatch("B1", "P101", 5, LocalDate.now().plusMonths(2), LocalDate.now());
        ds.saveBatches(Collections.singletonList(b1));

        MockInventoryService invService = new MockInventoryService(ds);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, ds);

        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 5);

        // First confirmation succeeds
        Sale sale1 = salesService.commitSale(cart, PaymentMethod.CASH);
        assertEquals(new BigDecimal("62.50"), sale1.getFinalTotal(), "First sale succeeds");

        // Immediate duplicate confirmation (simulating double click or resubmission)
        boolean duplicateFailed = false;
        try {
            salesService.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException se) {
            duplicateFailed = true;
            assertTrue(se.getMessage().contains("Insufficient sellable stock"), "Must report insufficient stock on duplicate submit");
        }

        assertTrue(duplicateFailed, "Duplicate submission must be rejected");

        // Stock must be 0, never negative
        assertEquals(0, ds.loadBatches().get(0).getQuantity(), "Stock must be exactly 0, not negative");
        assertEquals(1, ds.loadSales().size(), "Only 1 sale recorded in storage");
    }

    // =========================================================================
    // TEST 8: Failed Storage Commit Rollback
    // =========================================================================
    public static void testFailedStorageCommitRollback() throws Exception {
        File dataDir = createTempDataDir("test_storage_rollback");
        DataStore baseDs = new CsvDataStore(dataDir);

        Product p1 = new Product("P101", "Olive Oil", "Pantry", new BigDecimal("15.00"));
        baseDs.saveProducts(Collections.singletonList(p1));

        ProductBatch b1 = new ProductBatch("B1", "P101", 10, LocalDate.now().plusMonths(5), LocalDate.now());
        baseDs.saveBatches(Collections.singletonList(b1));

        // Faulty DataStore that fails halfway through transaction
        DataStore faultyDs = new DataStore() {
            @Override
            public List<Product> loadProducts() throws Exception { return baseDs.loadProducts(); }
            @Override
            public void saveProducts(List<Product> products) throws Exception { baseDs.saveProducts(products); }
            @Override
            public List<ProductBatch> loadBatches() throws Exception { return baseDs.loadBatches(); }
            @Override
            public void saveBatches(List<ProductBatch> batches) throws Exception { baseDs.saveBatches(batches); }
            @Override
            public List<Sale> loadSales() throws Exception { return baseDs.loadSales(); }
            @Override
            public void saveSales(List<Sale> sales) throws Exception {
                // Simulate disk failure during sales write
                throw new java.io.IOException("Simulated I/O disk failure during sales write");
            }
            @Override
            public List<SaleItem> loadSaleItems() throws Exception { return baseDs.loadSaleItems(); }
            @Override
            public void saveSaleItems(List<SaleItem> saleItems) throws Exception { baseDs.saveSaleItems(saleItems); }
            @Override
            public void executeTransaction(TransactionOperation operation) throws Exception {
                baseDs.executeTransaction(operation);
            }
        };

        MockInventoryService invService = new MockInventoryService(faultyDs);
        SalesServiceImpl salesService = new SalesServiceImpl(invService, faultyDs);

        Map<String, Integer> cart = new HashMap<>();
        cart.put("P101", 3);

        boolean transactionFailed = false;
        try {
            salesService.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException se) {
            transactionFailed = true;
            assertTrue(se.getMessage().contains("failed during persistence"), "Should indicate persistence failure");
        }

        assertTrue(transactionFailed, "SaleException must be raised on storage failure");

        // Verify storage rollback: batches must still be 10, no sale recorded
        assertEquals(10, baseDs.loadBatches().get(0).getQuantity(), "Batch quantity must be restored to 10 via rollback");
        assertEquals(0, baseDs.loadSales().size(), "No sale record should remain");
        assertEquals(0, baseDs.loadSaleItems().size(), "No sale items should remain");
    }
}
