# ShelfLife Master Test Execution Report (`TEST_REPORT.md`)

## 1. Test Suite Execution Summary

- **Test Suite**: [`shelflife.test.SystemAuditTestSuite`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/test/shelflife/test/SystemAuditTestSuite.java)
- **Execution Script**: `test.bat`
- **Total Scenarios**: 30
- **Passed**: 30
- **Failed**: 0
- **Pass Rate**: **100.0%**
- **Test Isolation**: Executed in dynamically allocated temporary directory (`Files.createTempDirectory`) with guaranteed zero side-effects on live store records.

---

## 2. Comprehensive 30-Scenario Test Results Matrix

| # | Test Scenario Name | Primary Target Under Test | Input / Test Condition | Expected Assertion | Actual Outcome | Status |
| :-: | :--- | :--- | :--- | :--- | :--- | :-: |
| **01** | Add Valid Product | `InventoryService.addProduct` | ID: `PROD-01`, Price: `180.00`, Thresh: `5` | Product persists, uppercase ID | Stored & retrieved as `PROD-01` | **PASS** |
| **02** | Reject Duplicate Product ID | `InventoryService.addProduct` | Add second `PROD-01` | `IllegalArgumentException` thrown | Blocked duplicate ID exception | **PASS** |
| **03** | Reject Invalid Prices & Quantities | Domain Validations | Price `-50.00`, Threshold `-2` | Rejects negative numeric values | Threw validation exceptions | **PASS** |
| **04** | Search and Edit Product | `InventoryService.searchProducts` | Name update to `"Organic Almond Milk"` | Matches query `"Organic"` | Found updated product | **PASS** |
| **05** | Add Multiple Batches for One Product | `InventoryService.addBatch` | Batches: `B1 (10)`, `B2 (15)`, `B3 (20)` | 3 distinct batches created | 3 batches created for `PROD-01` | **PASS** |
| **06** | Calculate Total Stock Correctly | `InventoryService.getTotalStock` | Sum across batches (10 + 15 + 20) | Equals 45 | Returned exactly 45 | **PASS** |
| **07** | Identify Low Stock Product | `InventoryService.isLowStock` | Product with stock 8, threshold 10 | Flags `isLowStock == true` | Accurately flagged low stock | **PASS** |
| **08** | Identify Expired & Near-Expiry Batches | `ExpiryService.isExpired` | Batch expired yesterday vs batch expiring today | Yesterday is expired; today is NOT expired | Business rule strictly verified | **PASS** |
| **09** | Allocate Stock Using FEFO | `SalesService.commitSale` | Batches: Oct 1, Nov 1, Dec 1 | Earliest expiry allocated first | Strictly sorted ascending | **PASS** |
| **10** | Complete Valid Sale | `SalesService.commitSale` | Sale of 20 units spanning 2 batches | Multi-batch deduction + receipt | Batch 1 drained (0), Batch 2 (5) | **PASS** |
| **11** | Reject Sale with Insufficient Stock | `SalesService.commitSale` | Request 100 units (only 15 available) | `SaleException` thrown | Rejected checkout with clear message | **PASS** |
| **12** | Cancel Checkout Without Stock Change | POS Checkout Cancellation | Fill cart, then discard before payment | Stock count unchanged (15 units) | Zero side-effects on inventory | **PASS** |
| **13** | Verify Sale Totals & Receipt Details | Receipt Arithmetic | Sum of `SaleItem.lineTotal` | Strictly equals `Sale.totalAmount` | Exact sum match (750.00 PKR) | **PASS** |
| **14** | Record Valid Wastage | `ExpiryService.recordWastage` | Write off 2 units of damaged batch | Batch reduced from 5 to 3 | Quantity reduced; wastage logged | **PASS** |
| **15** | Reject Wastage Exceeding Stock | `ExpiryService.recordWastage` | Request write-off 53 units (avail 3) | Rejects with `IllegalArgumentException` | Blocked excess wastage | **PASS** |
| **16** | Verify Dashboard & Report Figures | `ReportService` KPIs | Compute Products, Stock, Sales | Metrics strictly match database state | Products=3, Stock=76, Sales=750.00 | **PASS** |
| **17** | Restart Application & Verify Persistence | Database Reload | Simulate complete JVM restart | All records reloaded from CSV files | 100% data fidelity preserved | **PASS** |
| **18** | Handle Malformed CSV Files Safely | `CsvDataStore` Fault Tolerance | Missing columns, non-numeric price | Bypasses bad rows without crash | Valid records loaded cleanly | **PASS** |
| **19** | Handle Interrupted Transaction | Rollback Snapshot Engine | Mid-transaction runtime crash | Complete rollback to `.bak` snapshot | State 100% preserved | **PASS** |
| **20** | Verify UI Components & Navigation | `ShelfLifeApp` CardLayout | Navigate all 9 feature modules | Switch cards without exceptions | Smooth navigation across 9 panels | **PASS** |
| **21** | Supplier CRUD & Stock Intake | `SupplierService.receiveStock` | Supplier `SUP-TEST-01`, intake 40 units | Linked batch with supplierId & costPrice | Registered batch with cost Rs. 950 | **PASS** |
| **22** | Auth & Salted Password Hashing | `AuthService` RBAC | Login admin123, register cashier | SHA-256 salted hash, no plaintext | Salted hash verified in CSV | **PASS** |
| **23** | Return Processing & Restock | `ReturnsService.processReturn` | Return 2 units with `RESTOCK` | Refund calculated, stock 6 -> 8 | Refund Rs. 1,000.00; stock is 8 | **PASS** |
| **24** | Return as Wastage | `ReturnsService.processReturn` | Return 1 damaged unit with `WASTAGE` | Refund Rs. 150, stock NOT restocked | Stock stayed 5; wastage written | **PASS** |
| **25** | Settings Update & Persistence | `SettingsService` Configuration | Update store name, warning days (45) | Values persisted to `settings.csv` | Reloaded cleanly from disk | **PASS** |
| **26** | Activity Log Tracking | `ActivityLogService` Audit | Log `STOCK_ADJUST`, `PRICE_UPDATE` | Audit records with timestamps & actor | Persisted to `activity_logs.csv` | **PASS** |
| **27** | Backup Creation & Restore | `CsvDataStore.createBackup` | Backup folder, mutate record, restore | Original state 100% restored | Verified pre-mutation snapshot | **PASS** |
| **28** | Soft Deletion & Sale Prevention | Soft Delete & POS Checkout | Deactivate product, attempt sale | Blocked with `SaleException` | Inactive product cannot be sold | **PASS** |
| **29** | Barcode & SKU Search Matching | `InventoryService.searchProducts` | Search by EAN barcode & SKU code | Returns exact product hit | Matched barcode & SKU queries | **PASS** |
| **30** | Profit Report Calculation | `ReportService.getTotalProfit` | Sale 3 units at 1000, cost 700 | $\Delta \text{Profit} = 3000 - 2100 = 900$ | Exact Rs. 900.00 profit delta | **PASS** |

---

## 3. Raw Execution Log Output

```text
======================================================================
   Running ShelfLife Master System Audit and Test Suite
======================================================================
Compiling latest code and test suite...
======================================================================
   Compiling ShelfLife - Java OOP Inventory and Billing System
======================================================================
[1/2] Compiling main application source files...
[2/2] Compiling audit and regression test suite...

[SUCCESS] ShelfLife compiled successfully!
Run the application using: run.bat
Execute tests using: test.bat
===============================================================================
     SHELFLIFE SYSTEM AUDIT & REGRESSION TEST SUITE (30 SCENARIOS)            
===============================================================================
[PASS] Test 01: Add Valid Product -> Product stored and retrieved with uppercase ID 'PROD-01'
[PASS] Test 02: Reject Duplicate Product ID -> Successfully blocked duplicate: Product with ID 'PROD-01' already exists.
[PASS] Test 03: Reject Invalid Prices and Quantities -> Negative price, negative threshold, and negative batch quantity all rejected
[PASS] Test 04: Search and Edit Product -> Product updated and matched by search query 'Organic'
[PASS] Test 05: Add Multiple Batches For One Product -> Successfully created 3 batches for PROD-01
[PASS] Test 06: Calculate Total Stock Correctly -> Total stock correctly calculated as 45 units (10 + 15 + 20)
[PASS] Test 07: Identify Low Stock Product -> PROD-LOW accurately flagged as low stock (8 <= 10), PROD-01 safe
[PASS] Test 08: Identify Expired and Near-Expiry Batches -> Expired batch identified correctly; batch expiring TODAY is verified as NOT expired per business rules
[PASS] Test 09: Allocate Stock Using FEFO -> Batches ordered strictly by earliest expiry date ascending
[PASS] Test 10: Complete Valid Sale -> Sale SALE-20261027-160627-8285 committed. B-YOG-EARLY reduced to 0, B-YOG-MID reduced to 5
[PASS] Test 11: Reject Sale with Insufficient Stock -> Successfully rejected checkout: Insufficient sellable stock for 'Yogurt Cups'. Requested: 100, Available: 15
[PASS] Test 12: Cancel Checkout Without Changing Stock -> Cart cancelled in-memory with zero side-effects on inventory (15 units)
[PASS] Test 13: Verify Sale Totals and Receipt Details -> Sale SALE-20261027-160627-8285 line items sum (750.00) strictly matches totalAmount
[PASS] Test 14: Record Valid Wastage -> Wastage recorded. B-YOG-MID quantity reduced from 5 to 3
[PASS] Test 15: Reject Wastage Exceeding Available Stock -> Successfully rejected: Wastage quantity (53) cannot exceed available batch quantity (3).
[PASS] Test 16: Verify Dashboard and Report Figures -> Dashboard KPIs accurately computed: Products=3, Stock=76, Today's Sales=750.00
[PASS] Test 17: Restart Application and Verify Persistence -> All products, batches, sales, and wastage persisted and reloaded successfully from disk
Warning: Skipping malformed product record at line 4: Character N is neither a decimal digit number, decimal point, nor "e" notation exponential mark.
[PASS] Test 18: Handle Missing or Malformed CSV Files Safely -> Parsed 2 valid products while gracefully skipping corrupted lines without throwing fatal exception
[PASS] Test 19: Handle Interrupted Multi-File Transaction -> Transaction rolled back completely to previous snapshot. File integrity 100% preserved
[PASS] Test 20: Verify UI Components and Navigation -> CardLayout navigation across all 9 modules (Inventory, Suppliers, Expiry, Billing, Returns, Reports, Reorder, Activity, Settings) executes smoothly without exceptions
[PASS] Test 21: Supplier CRUD & Stock Intake -> Supplier registered, batch received with linked supplierId SUP-TEST-01, costPrice Rs. 950.00, and quantity 40
[PASS] Test 22: Auth & Role Permissions -> PBKDF2/SHA-256 salted hash verified, role CASHIER assigned, plaintext leak avoided
[PASS] Test 23: Return Processing & Restock -> Refund of Rs. 1,000.00 calculated accurately; inventory restocked from 6 back to 8 units
[PASS] Test 24: Return as Wastage -> Damaged return refunded Rs. 150.00; stock NOT placed back into sellable inventory (stayed 5), wastage logged
[PASS] Test 25: Settings Update & Persistence -> Store name, warning days (45), and tax rate persisted to settings.csv and reloaded cleanly
[PASS] Test 26: Activity Log Tracking -> Audit actions logged with actor 'admin', timestamps, and action metadata persisted to activity_log.csv
[PASS] Test 27: Backup Creation & Restore -> Complete directory backup generated, verified, and restored cleanly with pre-restore safety snapshot
[PASS] Test 28: Soft Deletion & Inactive Prevention -> Deactivated product successfully rejected from billing checkout: Cannot sell inactive product: 'Discontinued Soda 330ml' (ID: PROD-DEACT).
[PASS] Test 29: Barcode & SKU Search Matching -> Fast lookup matching verified for EAN/UPC barcode ('8964001234567') and SKU ('SKU-HAIR-001')
[PASS] Test 30: Profit Report Calculation -> Accurate Net Profit Delta: Revenue (Rs. 3,000.00) - COGS (Rs. 2,100.00) = Rs. 900.00 (Total Store Profit: Rs. 800.00)

===============================================================================
AUDIT SUMMARY: 30 PASSED | 0 FAILED | Total: 30
===============================================================================
AUDIT STATUS: ALL 30 SCENARIOS PASSED WITH ZERO DEFECTS (100% SUCCESS)
```
