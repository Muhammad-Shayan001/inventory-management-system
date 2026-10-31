package shelflife.test;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;
import shelflife.service.InventoryService;
import shelflife.service.InventoryServiceImpl;
import shelflife.storage.InMemoryDataStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Automated test suite for Member 1: Product & Inventory Management.
 * Verifies business rules, validation, edge cases, and protected deletion.
 */
public class ProductInventoryTest {

    private InMemoryDataStore dataStore;
    private InventoryService inventoryService;
    private int passedTests = 0;
    private int totalTests = 0;

    public static void main(String[] args) {
        ProductInventoryTest runner = new ProductInventoryTest();
        runner.runAllTests();
    }

    public void runAllTests() {
        System.out.println("==========================================================");
        System.out.println("    SHELFLIFE MEMBER 1 AUTOMATED TEST SUITE EXECUTION    ");
        System.out.println("==========================================================");

        testAddValidProduct();
        testRejectDuplicateProductId();
        testRejectInvalidProductFields();
        testSearchProducts();
        testUpdateProduct();
        testAddBatchToExistingProduct();
        testRejectBatchForNonExistentProduct();
        testRejectDuplicateBatchId();
        testRejectInvalidBatchQuantity();
        testStockCalculationsAndLowStockStatus();
        testProtectedDeletionBlockedByActiveBatches();
        testProtectedDeletionBlockedByHistoricalSale();
        testProtectedDeletionBlockedByHistoricalWastage();
        testSuccessfulDeletionWhenNoDependencies();
        testExpiryDateBoundaryRule();
        testAutomaticUppercaseIdNormalization();

        System.out.println("==========================================================");
        System.out.printf("TEST SUMMARY: %d / %d tests passed successfully.\n", passedTests, totalTests);
        System.out.println("==========================================================");

        if (passedTests != totalTests) {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED WITH ZERO ERRORS!");
        }
    }

    private void setUp() {
        dataStore = new InMemoryDataStore();
        inventoryService = new InventoryServiceImpl(dataStore);
    }

    private void recordPass(String testName) {
        passedTests++;
        totalTests++;
        System.out.println("[PASS] " + testName);
    }

    private void recordFail(String testName, String reason) {
        totalTests++;
        System.err.println("[FAIL] " + testName + " -> " + reason);
    }

    // 1. Add valid product
    public void testAddValidProduct() {
        setUp();
        String testName = "testAddValidProduct";
        try {
            Product p = new Product("P001", "Full Cream Milk", "Dairy", new BigDecimal("180.00"), 10);
            inventoryService.addProduct(p);

            Product retrieved = inventoryService.getProductById("P001");
            if (retrieved != null && "Full Cream Milk".equals(retrieved.getName()) &&
                    new BigDecimal("180.00").compareTo(retrieved.getSellingPrice()) == 0) {
                recordPass(testName);
            } else {
                recordFail(testName, "Retrieved product did not match saved product.");
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 2. Reject duplicate product ID
    public void testRejectDuplicateProductId() {
        setUp();
        String testName = "testRejectDuplicateProductId";
        try {
            inventoryService.addProduct(new Product("P001", "Full Cream Milk", "Dairy", new BigDecimal("180.00"), 10));

            // Attempting to add with same ID
            try {
                inventoryService.addProduct(new Product("P001", "Skimmed Milk", "Dairy", new BigDecimal("170.00"), 5));
                recordFail(testName, "Expected IllegalArgumentException for duplicate product ID, but none was thrown.");
            } catch (IllegalArgumentException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 3. Reject invalid product fields
    public void testRejectInvalidProductFields() {
        setUp();
        String testName = "testRejectInvalidProductFields";
        try {
            // Null/Blank name
            try {
                new Product("P002", "", "Dairy", new BigDecimal("100"), 5);
                recordFail(testName, "Expected exception on blank name.");
                return;
            } catch (IllegalArgumentException ignored) {}

            // Zero price
            try {
                new Product("P002", "Butter", "Dairy", BigDecimal.ZERO, 5);
                recordFail(testName, "Expected exception on zero price.");
                return;
            } catch (IllegalArgumentException ignored) {}

            // Negative price
            try {
                new Product("P002", "Butter", "Dairy", new BigDecimal("-50.00"), 5);
                recordFail(testName, "Expected exception on negative price.");
                return;
            } catch (IllegalArgumentException ignored) {}

            // Negative threshold
            try {
                new Product("P002", "Butter", "Dairy", new BigDecimal("50.00"), -1);
                recordFail(testName, "Expected exception on negative threshold.");
                return;
            } catch (IllegalArgumentException ignored) {}

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 4. Search products by ID and name
    public void testSearchProducts() {
        setUp();
        String testName = "testSearchProducts";
        try {
            inventoryService.addProduct(new Product("P001", "Full Cream Milk", "Dairy", new BigDecimal("180.00"), 10));
            inventoryService.addProduct(new Product("P002", "Whole Wheat Bread", "Bakery", new BigDecimal("95.00"), 15));
            inventoryService.addProduct(new Product("P003", "Almond Milk", "Dairy", new BigDecimal("350.00"), 5));

            // Search by term "Milk" (should match P001 and P003)
            List<Product> milkResults = inventoryService.searchProducts("milk");
            if (milkResults.size() != 2) {
                recordFail(testName, "Expected 2 matches for 'milk', found: " + milkResults.size());
                return;
            }

            // Search by ID "P002"
            List<Product> idResults = inventoryService.searchProducts("p002");
            if (idResults.size() != 1 || !"Whole Wheat Bread".equals(idResults.get(0).getName())) {
                recordFail(testName, "Expected 1 match for 'p002'.");
                return;
            }

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 5. Update product information
    public void testUpdateProduct() {
        setUp();
        String testName = "testUpdateProduct";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));

            Product updated = new Product("P001", "Fresh Farm Milk", "Dairy", new BigDecimal("195.00"), 12);
            inventoryService.updateProduct(updated);

            Product retrieved = inventoryService.getProductById("P001");
            if ("Fresh Farm Milk".equals(retrieved.getName()) &&
                    new BigDecimal("195.00").compareTo(retrieved.getSellingPrice()) == 0 &&
                    retrieved.getLowStockThreshold() == 12) {
                recordPass(testName);
            } else {
                recordFail(testName, "Updated fields did not match.");
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 6. Add batch to existing product
    public void testAddBatchToExistingProduct() {
        setUp();
        String testName = "testAddBatchToExistingProduct";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));

            LocalDate futureDate = LocalDate.now().plusDays(10);
            ProductBatch batch = new ProductBatch("B001", "P001", 25, futureDate);
            inventoryService.addBatch(batch);

            ProductBatch retrieved = inventoryService.getBatchById("B001");
            if (retrieved != null && retrieved.getQuantity() == 25 && "P001".equals(retrieved.getProductId())) {
                recordPass(testName);
            } else {
                recordFail(testName, "Retrieved batch did not match saved batch.");
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 7. Reject batch referencing nonexistent product
    public void testRejectBatchForNonExistentProduct() {
        setUp();
        String testName = "testRejectBatchForNonExistentProduct";
        try {
            ProductBatch orphanBatch = new ProductBatch("B999", "P999_NON_EXISTENT", 10, LocalDate.now().plusDays(5));
            try {
                inventoryService.addBatch(orphanBatch);
                recordFail(testName, "Expected exception when adding batch to nonexistent product.");
            } catch (IllegalArgumentException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 8. Reject duplicate batch ID
    public void testRejectDuplicateBatchId() {
        setUp();
        String testName = "testRejectDuplicateBatchId";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));
            inventoryService.addBatch(new ProductBatch("B001", "P001", 10, LocalDate.now().plusDays(5)));

            try {
                inventoryService.addBatch(new ProductBatch("B001", "P001", 20, LocalDate.now().plusDays(10)));
                recordFail(testName, "Expected exception when adding batch with duplicate ID.");
            } catch (IllegalArgumentException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 9. Reject invalid batch quantity
    public void testRejectInvalidBatchQuantity() {
        setUp();
        String testName = "testRejectInvalidBatchQuantity";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));

            // Negative quantity in model
            try {
                new ProductBatch("B001", "P001", -5, LocalDate.now().plusDays(5));
                recordFail(testName, "Expected exception on negative batch quantity.");
                return;
            } catch (IllegalArgumentException ignored) {}

            // Zero quantity in addBatch service call
            try {
                inventoryService.addBatch(new ProductBatch("B001", "P001", 0, LocalDate.now().plusDays(5)));
                recordFail(testName, "Expected exception on zero quantity addition.");
                return;
            } catch (IllegalArgumentException ignored) {}

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 10. Stock calculations and low-stock threshold
    public void testStockCalculationsAndLowStockStatus() {
        setUp();
        String testName = "testStockCalculationsAndLowStockStatus";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 15));

            // Initially 0 stock -> low stock = true
            if (inventoryService.getTotalStock("P001") != 0 || !inventoryService.isLowStock("P001")) {
                recordFail(testName, "Initial stock should be 0 and marked low stock.");
                return;
            }

            // Add batch of 10 units -> stock 10 <= threshold 15 -> still low stock
            inventoryService.addBatch(new ProductBatch("B001", "P001", 10, LocalDate.now().plusDays(5)));
            if (inventoryService.getTotalStock("P001") != 10 || !inventoryService.isLowStock("P001")) {
                recordFail(testName, "Stock should be 10 and still low stock.");
                return;
            }

            // Add another batch of 10 units -> stock 20 > threshold 15 -> not low stock
            inventoryService.addBatch(new ProductBatch("B002", "P001", 10, LocalDate.now().plusDays(10)));
            if (inventoryService.getTotalStock("P001") != 20 || inventoryService.isLowStock("P001")) {
                recordFail(testName, "Stock should be 20 and not low stock.");
                return;
            }

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 11. Protected deletion: blocked when active batches exist
    public void testProtectedDeletionBlockedByActiveBatches() {
        setUp();
        String testName = "testProtectedDeletionBlockedByActiveBatches";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));
            inventoryService.addBatch(new ProductBatch("B001", "P001", 5, LocalDate.now().plusDays(5)));

            if (inventoryService.canDeleteProduct("P001")) {
                recordFail(testName, "canDeleteProduct should return false when batches exist.");
                return;
            }

            try {
                inventoryService.deleteProduct("P001");
                recordFail(testName, "deleteProduct should throw IllegalStateException when batches exist.");
            } catch (IllegalStateException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 12. Protected deletion: blocked when historical sales exist
    public void testProtectedDeletionBlockedByHistoricalSale() {
        setUp();
        String testName = "testProtectedDeletionBlockedByHistoricalSale";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));
            // Simulate a historical sale item for P001 in DataStore
            dataStore.addTestSaleItem(new SaleItem("S001", "P001", "B001", 2, new BigDecimal("180.00"), new BigDecimal("360.00")));

            if (inventoryService.canDeleteProduct("P001")) {
                recordFail(testName, "canDeleteProduct should return false when historical sales exist.");
                return;
            }

            try {
                inventoryService.deleteProduct("P001");
                recordFail(testName, "deleteProduct should throw IllegalStateException when historical sale exists.");
            } catch (IllegalStateException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 13. Protected deletion: blocked when historical wastage exists
    public void testProtectedDeletionBlockedByHistoricalWastage() {
        setUp();
        String testName = "testProtectedDeletionBlockedByHistoricalWastage";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));
            inventoryService.addBatch(new ProductBatch("B001", "P001", 5, LocalDate.now().minusDays(1)));
            dataStore.addTestWastageRecord(new WastageRecord("W001", "B001", 5, LocalDateTime.now(), "Spoiled"));

            if (inventoryService.canDeleteProduct("P001")) {
                recordFail(testName, "canDeleteProduct should return false when wastage references batch.");
                return;
            }

            try {
                inventoryService.deleteProduct("P001");
                recordFail(testName, "deleteProduct should throw IllegalStateException.");
            } catch (IllegalStateException ex) {
                recordPass(testName);
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 14. Successful deletion when no dependencies exist
    public void testSuccessfulDeletionWhenNoDependencies() {
        setUp();
        String testName = "testSuccessfulDeletionWhenNoDependencies";
        try {
            inventoryService.addProduct(new Product("P001", "Milk", "Dairy", new BigDecimal("180.00"), 10));

            if (!inventoryService.canDeleteProduct("P001")) {
                recordFail(testName, "canDeleteProduct should return true when no batches or historical dependencies exist.");
                return;
            }

            inventoryService.deleteProduct("P001");

            if (inventoryService.getProductById("P001") == null) {
                recordPass(testName);
            } else {
                recordFail(testName, "Product should have been removed from data store.");
            }
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 15. Expiry boundary rule verification (Expiring today is NOT expired)
    public void testExpiryDateBoundaryRule() {
        setUp();
        String testName = "testExpiryDateBoundaryRule";
        try {
            LocalDate today = LocalDate.now();
            ProductBatch batchYesterday = new ProductBatch("B_PAST", "P001", 10, today.minusDays(1));
            ProductBatch batchToday = new ProductBatch("B_TODAY", "P001", 10, today);
            ProductBatch batchTomorrow = new ProductBatch("B_FUTURE", "P001", 10, today.plusDays(1));

            if (!batchYesterday.isExpired(today)) {
                recordFail(testName, "Batch expiring yesterday must be marked expired.");
                return;
            }

            // Project Rule: A batch expiring today is NOT considered expired
            if (batchToday.isExpired(today)) {
                recordFail(testName, "Batch expiring today must NOT be considered expired per project rule.");
                return;
            }

            if (batchTomorrow.isExpired(today)) {
                recordFail(testName, "Batch expiring tomorrow must NOT be marked expired.");
                return;
            }

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }

    // 16. Automatic Uppercase ID Normalization
    public void testAutomaticUppercaseIdNormalization() {
        setUp();
        String testName = "testAutomaticUppercaseIdNormalization";
        try {
            // Add product with lowercase id
            Product p = new Product("  p099  ", "Almond Butter", "Spreads", new BigDecimal("450.00"), 5);
            if (!"P099".equals(p.getProductId())) {
                recordFail(testName, "Product constructor did not normalize productId to P099: " + p.getProductId());
                return;
            }
            inventoryService.addProduct(p);

            // Verify retrieval works with both lowercase and uppercase lookup
            Product retrievedUpper = inventoryService.getProductById("P099");
            Product retrievedLower = inventoryService.getProductById("p099");
            if (retrievedUpper == null || retrievedLower == null || !"P099".equals(retrievedUpper.getProductId())) {
                recordFail(testName, "Failed to retrieve product with case-insensitive lookups.");
                return;
            }

            // Add batch with lowercase batchId and lowercase productId
            ProductBatch b = new ProductBatch("  b999  ", "  p099  ", 10, LocalDate.now().plusMonths(3));
            if (!"B999".equals(b.getBatchId()) || !"P099".equals(b.getProductId())) {
                recordFail(testName, "Batch constructor did not normalize IDs to uppercase: " + b);
                return;
            }
            inventoryService.addBatch(b);

            ProductBatch retrievedBatch = inventoryService.getBatchById("b999");
            if (retrievedBatch == null || !"B999".equals(retrievedBatch.getBatchId())) {
                recordFail(testName, "Failed to retrieve batch with lowercase lookup.");
                return;
            }

            recordPass(testName);
        } catch (Exception e) {
            recordFail(testName, e.getMessage());
        }
    }
}
