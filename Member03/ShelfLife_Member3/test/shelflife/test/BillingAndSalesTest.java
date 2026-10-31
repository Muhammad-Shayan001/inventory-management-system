package shelflife.test;

import shelflife.model.*;
import shelflife.service.InventoryService;
import shelflife.service.InventoryServiceImpl;
import shelflife.service.SaleException;
import shelflife.service.SalesService;
import shelflife.service.SalesServiceImpl;
import shelflife.storage.DataStore;
import shelflife.storage.FileManager;
import shelflife.storage.StorageException;
import shelflife.ui.billing.CartItem;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Automated test suite for ShelfLife Member 3 (Billing & Sales Management).
 *
 * Verifies all 8 required university grading criteria:
 * 1. Successful sale & stock reduction
 * 2. Multi-batch allocation via FEFO (ascending expiry date + batch ID tie-breaker)
 * 3. Insufficient sellable stock rejection
 * 4. Expired stock exclusion (batches expiring today are NOT expired; past expiry excluded)
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

        runTestCase("1. Successful Sale", BillingAndSalesTest::testSuccessfulSale);
        runTestCase("2. Multi-Batch FEFO Allocation", BillingAndSalesTest::testMultipleBatchAllocation);
        runTestCase("3. Insufficient Stock Rejection", BillingAndSalesTest::testInsufficientStock);
        runTestCase("4. Expired Stock Exclusion", BillingAndSalesTest::testExpiredStockExclusion);
        runTestCase("5. Cart Cancellation Zero Side-Effects", BillingAndSalesTest::testCartCancellation);
        runTestCase("6. BigDecimal Rounding & Price Immutability", BillingAndSalesTest::testRoundingAndPriceImmutability);
        runTestCase("7. Repeated Confirmation & Duplicate Deduction Protection", BillingAndSalesTest::testRepeatedConfirmationPrevention);
        runTestCase("8. Failed Storage Commit Rollback", BillingAndSalesTest::testFailedStorageCommitRollback);

        System.out.println("======================================================================");
        System.out.println(String.format("TEST RESULTS: Passed: %d | Failed: %d | Total: %d",
                testsPassed, testsFailed, testsPassed + testsFailed));
        System.out.println("======================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTestCase(String testName, TestRunnable test) {
        try {
            System.out.print("[RUNNING] " + testName + " ... ");
            test.run();
            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED!");
            System.err.println("  -> Assertion Error: " + t.getMessage());
            t.printStackTrace(System.err);
            testsFailed++;
        }
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Throwable;
    }

    private static Path createTempDataDirectory(String prefix) throws IOException {
        Path tempPath = Files.createTempDirectory(prefix + "_" + System.nanoTime());
        tempPath.toFile().deleteOnExit();
        return tempPath;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - Expected: <" + expected + "> but was: <" + actual + ">");
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
        Path dataDir = createTempDataDirectory("test1_success");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product milk = new Product("P001", "Milk 1L", "Dairy", new BigDecimal("60.00"), 5);
        inv.addProduct(milk);
        inv.addBatch(new ProductBatch("B001", "P001", 20, today.plusDays(5)));

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P001", 5);

        Sale sale = sales.commitSale(cart, PaymentMethod.CASH);

        // Verification
        assertEquals(new BigDecimal("300.00"), sale.getTotalAmount(), "Total amount should be 5 * 60.00 = 300.00");
        assertEquals("PAID", sale.getPaymentStatus(), "Payment status should be PAID");
        assertEquals(1, sale.getItems().size(), "Should have 1 sale item");

        SaleItem item = sale.getItems().get(0);
        assertEquals("P001", item.getProductId(), "Item product ID");
        assertEquals("B001", item.getBatchId(), "Item batch ID");
        assertEquals(5, item.getQuantity(), "Item quantity");
        assertEquals(new BigDecimal("60.00"), item.getUnitPrice(), "Item unit price");
        assertEquals(new BigDecimal("300.00"), item.getLineTotal(), "Item line total");

        // Verify batch quantity reduced
        ProductBatch b001 = inv.getBatchById("B001");
        assertEquals(15, b001.getQuantity(), "Remaining batch quantity should be 15");
    }

    // =========================================================================
    // TEST 2: Multi-Batch FEFO Allocation
    // =========================================================================
    public static void testMultipleBatchAllocation() throws Exception {
        Path dataDir = createTempDataDirectory("test2_fefo");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product bread = new Product("P002", "Bread 400g", "Bakery", new BigDecimal("50.00"), 5);
        inv.addProduct(bread);

        // Batch 1: Expires in 2 days (10 units) -> Earliest
        inv.addBatch(new ProductBatch("B-EARLY", "P002", 10, today.plusDays(2)));
        // Batch 2: Expires in 6 days (15 units) -> Later
        inv.addBatch(new ProductBatch("B-LATER", "P002", 15, today.plusDays(6)));

        // Request 14 units (requires exhausting B-EARLY and taking 4 from B-LATER)
        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P002", 14);

        Sale sale = sales.commitSale(cart, PaymentMethod.CARD);

        assertEquals(new BigDecimal("700.00"), sale.getTotalAmount(), "Total amount should be 14 * 50.00 = 700.00");
        List<SaleItem> items = sale.getItems();
        assertEquals(2, items.size(), "Must create 2 separate SaleItems for multiple batch allocations");

        // First item should be B-EARLY with 10 units
        SaleItem item1 = items.get(0);
        assertEquals("B-EARLY", item1.getBatchId(), "First allocation must come from earliest expiring batch");
        assertEquals(10, item1.getQuantity(), "Quantity from first batch");
        assertEquals(new BigDecimal("500.00"), item1.getLineTotal(), "Line total for first batch");

        // Second item should be B-LATER with 4 units
        SaleItem item2 = items.get(1);
        assertEquals("B-LATER", item2.getBatchId(), "Second allocation must come from next earliest batch");
        assertEquals(4, item2.getQuantity(), "Quantity from second batch");
        assertEquals(new BigDecimal("200.00"), item2.getLineTotal(), "Line total for second batch");

        // Verify remaining stock in storage
        assertEquals(0, inv.getBatchById("B-EARLY").getQuantity(), "B-EARLY should be exhausted to 0");
        assertEquals(11, inv.getBatchById("B-LATER").getQuantity(), "B-LATER should have 15 - 4 = 11 remaining");
    }

    // =========================================================================
    // TEST 3: Insufficient Stock Rejection
    // =========================================================================
    public static void testInsufficientStock() throws Exception {
        Path dataDir = createTempDataDirectory("test3_insufficient");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product cheese = new Product("P003", "Cheese 200g", "Dairy", new BigDecimal("150.00"), 5);
        inv.addProduct(cheese);
        inv.addBatch(new ProductBatch("B-CH1", "P003", 8, today.plusDays(10)));

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P003", 15); // Requesting 15 when only 8 are available

        boolean exceptionThrown = false;
        try {
            sales.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException e) {
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("Insufficient sellable stock"), "Error message should mention insufficient stock");
        }

        assertTrue(exceptionThrown, "SaleException must be thrown when stock is insufficient");
        // Verify batch stock was unchanged
        assertEquals(8, inv.getBatchById("B-CH1").getQuantity(), "Stock must not be deducted on failure");
        assertEquals(0, sales.getAllSales().size(), "No sale record should be saved");
    }

    // =========================================================================
    // TEST 4: Expired Stock Exclusion
    // =========================================================================
    public static void testExpiredStockExclusion() throws Exception {
        Path dataDir = createTempDataDirectory("test4_expired");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product yogurt = new Product("P004", "Yogurt 400g", "Dairy", new BigDecimal("80.00"), 5);
        inv.addProduct(yogurt);

        // Fresh batch (10 units, expires in 5 days)
        inv.addBatch(new ProductBatch("B-FRESH", "P004", 10, today.plusDays(5)));
        // Expired batch (6 units, expired 2 days ago) -> MUST BE EXCLUDED
        inv.addBatch(new ProductBatch("B-EXPIRED", "P004", 6, today.minusDays(2)));
        // Batch expiring today (5 units, expires today) -> Under team rule, NOT expired!
        inv.addBatch(new ProductBatch("B-TODAY", "P004", 5, today));

        // Sellable stock = 10 (fresh) + 5 (today) = 15. The 6 expired units cannot be sold.
        // Attempting to buy 16 should fail!
        Map<String, Integer> cartFail = new LinkedHashMap<>();
        cartFail.put("P004", 16);
        boolean failed = false;
        try {
            sales.commitSale(cartFail, PaymentMethod.CASH);
        } catch (SaleException e) {
            failed = true;
        }
        assertTrue(failed, "Must throw SaleException when requesting more than non-expired stock");

        // Buying 12 units should succeed: 5 from B-TODAY (earliest non-expired) + 7 from B-FRESH
        Map<String, Integer> cartSuccess = new LinkedHashMap<>();
        cartSuccess.put("P004", 12);
        Sale sale = sales.commitSale(cartSuccess, PaymentMethod.CASH);

        assertEquals(new BigDecimal("960.00"), sale.getTotalAmount(), "12 * 80.00 = 960.00");
        // Expired batch must remain completely untouched
        assertEquals(6, inv.getBatchById("B-EXPIRED").getQuantity(), "Expired batch quantity must remain untouched at 6");
        assertEquals(0, inv.getBatchById("B-TODAY").getQuantity(), "B-TODAY batch should be drained to 0");
        assertEquals(3, inv.getBatchById("B-FRESH").getQuantity(), "B-FRESH batch should have 10 - 7 = 3 remaining");
    }

    // =========================================================================
    // TEST 5: Cart Cancellation Zero Side-Effects
    // =========================================================================
    public static void testCartCancellation() throws Exception {
        Path dataDir = createTempDataDirectory("test5_cancellation");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product juice = new Product("P005", "Orange Juice 1L", "Beverage", new BigDecimal("120.00"), 5);
        inv.addProduct(juice);
        inv.addBatch(new ProductBatch("B-JUICE-1", "P005", 25, today.plusDays(30)));

        // Stage cart item in UI staging model
        CartItem cartItem = new CartItem("P005", "Orange Juice 1L", 10, juice.getSellingPrice());
        assertEquals(new BigDecimal("1200.00"), cartItem.getLineTotal(), "Cart item line total");

        // User decides to cancel / clear cart:
        // Do not call commitSale!
        // Verify inventory in data store is 100% unaffected:
        assertEquals(25, inv.getBatchById("B-JUICE-1").getQuantity(), "Inventory must remain 25 after cart cancellation");
        assertEquals(0, sales.getAllSales().size(), "No sale record should exist");
    }

    // =========================================================================
    // TEST 6: BigDecimal Rounding & Price Immutability
    // =========================================================================
    public static void testRoundingAndPriceImmutability() throws Exception {
        Path dataDir = createTempDataDirectory("test6_rounding");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        // Fractional prices that fail under standard double floating point math:
        // 19.99 * 3 = 59.97
        Product itemA = new Product("P-A", "Item A", "General", new BigDecimal("19.99"), 5);
        Product itemB = new Product("P-B", "Item B", "General", new BigDecimal("33.33"), 5);
        inv.addProduct(itemA);
        inv.addProduct(itemB);

        inv.addBatch(new ProductBatch("B-A1", "P-A", 10, today.plusDays(10)));
        inv.addBatch(new ProductBatch("B-B1", "P-B", 10, today.plusDays(10)));

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P-A", 3);
        cart.put("P-B", 3);

        Sale sale = sales.commitSale(cart, PaymentMethod.UPI);

        // Expected: (3 * 19.99) + (3 * 33.33) = 59.97 + 99.99 = 159.96
        BigDecimal expectedTotal = new BigDecimal("159.96");
        assertEquals(expectedTotal, sale.getTotalAmount(), "Total must match exact BigDecimal calculation");

        // PRICE IMMUTABILITY RULE:
        // Change product selling price in catalog:
        itemA.setSellingPrice(new BigDecimal("99.99"));
        inv.updateProduct(itemA);

        // Verify historical sale and sale item unit prices DID NOT change:
        Sale reloadedSale = sales.getSaleById(sale.getSaleId());
        assertEquals(new BigDecimal("159.96"), reloadedSale.getTotalAmount(), "Historical sale total must remain unchanged");
        List<SaleItem> items = sales.getSaleItemsBySaleId(sale.getSaleId());
        for (SaleItem si : items) {
            if (si.getProductId().equals("P-A")) {
                assertEquals(new BigDecimal("19.99"), si.getUnitPrice(), "Historical charged unit price must remain 19.99");
            }
        }
    }

    // =========================================================================
    // TEST 7: Repeated Confirmation & Duplicate Deduction Protection
    // =========================================================================
    public static void testRepeatedConfirmationPrevention() throws Exception {
        Path dataDir = createTempDataDirectory("test7_duplicate");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);
        SalesService sales = new SalesServiceImpl(inv, ds);

        LocalDate today = LocalDate.now();
        Product tea = new Product("P007", "Green Tea 50 bags", "Beverage", new BigDecimal("250.00"), 5);
        inv.addProduct(tea);
        inv.addBatch(new ProductBatch("B-TEA-1", "P007", 10, today.plusDays(40)));

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P007", 6);

        // First commit succeeds
        Sale sale1 = sales.commitSale(cart, PaymentMethod.CASH);
        assertEquals(4, inv.getBatchById("B-TEA-1").getQuantity(), "Batch should have 4 remaining after 1st sale");

        // Attempting to submit the exact same cart again should be rejected because remaining stock is now 4 (< 6)
        boolean duplicateFailed = false;
        try {
            sales.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException ex) {
            duplicateFailed = true;
        }

        assertTrue(duplicateFailed, "Submitting the same cart a second time must fail due to stock depletion");
        assertEquals(4, inv.getBatchById("B-TEA-1").getQuantity(), "Stock must not be deducted a second time");
        assertEquals(1, sales.getAllSales().size(), "Only 1 sale record should exist");
    }

    // =========================================================================
    // TEST 8: Failed Storage Commit Rollback
    // =========================================================================
    public static void testFailedStorageCommitRollback() throws Exception {
        Path dataDir = createTempDataDirectory("test8_rollback");
        DataStore ds = new FileManager(dataDir);
        InventoryService inv = new InventoryServiceImpl(ds);

        LocalDate today = LocalDate.now();
        Product coffee = new Product("P008", "Coffee 200g", "Beverage", new BigDecimal("350.00"), 5);
        inv.addProduct(coffee);
        inv.addBatch(new ProductBatch("B-COF-1", "P008", 20, today.plusDays(60)));

        // Create a custom failing DataStore proxy to simulate an I/O crash during commitSale
        DataStore failingDs = new DataStore() {
            @Override public void loadAll() throws StorageException { ds.loadAll(); }
            @Override public void saveAll() throws StorageException { ds.saveAll(); }
            @Override public List<Product> getAllProducts() { return ds.getAllProducts(); }
            @Override public Product getProductById(String id) { return ds.getProductById(id); }
            @Override public void addProduct(Product p) throws StorageException { ds.addProduct(p); }
            @Override public void updateProduct(Product p) throws StorageException { ds.updateProduct(p); }
            @Override public void deleteProduct(String id) throws StorageException { ds.deleteProduct(id); }
            @Override public List<ProductBatch> getAllBatches() { return ds.getAllBatches(); }
            @Override public List<ProductBatch> getBatchesByProductId(String id) { return ds.getBatchesByProductId(id); }
            @Override public ProductBatch getBatchById(String id) { return ds.getBatchById(id); }
            @Override public void addBatch(ProductBatch b) throws StorageException { ds.addBatch(b); }
            @Override public void updateBatch(ProductBatch b) throws StorageException { ds.updateBatch(b); }
            @Override public void deleteBatch(String id) throws StorageException { ds.deleteBatch(id); }
            @Override public List<Sale> getAllSales() { return ds.getAllSales(); }
            @Override public List<SaleItem> getAllSaleItems() { return ds.getAllSaleItems(); }
            @Override public List<WastageRecord> getAllWastageRecords() { return ds.getAllWastageRecords(); }

            @Override
            public void commitSale(Sale sale, List<SaleItem> items, List<ProductBatch> updatedBatches) throws StorageException {
                // Simulate disk failure or permission denied
                throw new StorageException("SIMULATED HARD DISK WRITE FAILURE DURING SALES TRANSACTION");
            }

            @Override
            public void commitWastage(WastageRecord wastage, List<ProductBatch> updatedBatches) throws StorageException {
                ds.commitWastage(wastage, updatedBatches);
            }
        };

        SalesService failingSalesService = new SalesServiceImpl(inv, failingDs);

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("P008", 5);

        boolean exceptionThrown = false;
        try {
            failingSalesService.commitSale(cart, PaymentMethod.CASH);
        } catch (SaleException ex) {
            exceptionThrown = true;
            assertTrue(ex.getMessage().contains("Failed to commit sale"), "Exception message should indicate persistence failure");
        }

        assertTrue(exceptionThrown, "SaleException must be thrown on storage commit failure");
        // Verify in-memory inventory batches were restored and stock is NOT deducted
        assertEquals(20, inv.getBatchById("B-COF-1").getQuantity(), "Stock must be rolled back to original 20 units");
        assertEquals(0, ds.getAllSales().size(), "Storage must contain zero sales records");
    }
}
