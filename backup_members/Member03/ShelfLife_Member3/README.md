# ShelfLife — Member 3: Billing & Sales Management Module

**Course:** Object-Oriented Programming (Java 8+ / Swing / Apache NetBeans)  
**System:** ShelfLife Food Inventory & POS Management System  
**Role:** Member 3 (Billing & Sales Management Architect)  

---

## 1. Module Overview & Architectural Responsibilities

The **Billing and Sales Management Module** serves as the point-of-sale checkout engine for ShelfLife. It bridges the cashier user interface with the underlying inventory batch tracking and persistence layer, ensuring strict financial accuracy and transactional consistency.

### Key Architectural Boundaries:
* **UI Layer (`shelflife.ui.billing`):**
  * `BillingPanel`: Provides product selection, stock feedback, quantity configuration, in-memory cart staging, summary calculations, confirmation modals, and receipt triggering.
  * `ReceiptDialog`: Modal dialog rendering a formatted, printable customer sales receipt with multi-batch breakdowns.
  * `SalesHistoryDialog`: Modal window to browse past sales and view historical receipts.
  * `CartItem`: Lightweight UI presentation model.
* **Service Layer (`shelflife.service`):**
  * `SalesService` & `SalesServiceImpl`: Owns final stock re-validation at confirmation time, FEFO (First-Expiring, First-Out) batch allocation, creation of atomic `Sale` and `SaleItem` records, and rollback coordination.
  * `SaleException`: Dedicated exception for business rule violations.
* **Model Layer (`shelflife.model`):**
  * `Sale`: Captures overall transaction details (`saleId`, `saleDateTime`, `totalAmount`, `paymentMethod`, `paymentStatus`, `items`).
  * `SaleItem`: Identifies the exact batch from which units were drawn, actual unit price charged, and line total.
  * `PaymentMethod` & `PaymentStatus`: Enums for standard transaction categorization.
* **Storage Layer (`shelflife.storage`):**
  * Shared `DataStore` and `FileManager` with atomic CSV writes and transaction rollback.

---

## 2. Directory & Package Structure

```
ShelfLife_Member3/
├── build.xml                           # Ant build script for Apache NetBeans
├── nbproject/                          # NetBeans project configuration
│   ├── project.xml
│   └── project.properties
├── data/                               # Local UTF-8 CSV persistence files
│   ├── products.csv
│   ├── batches.csv
│   ├── sales.csv
│   ├── sale_items.csv
│   └── wastage.csv
├── src/
│   └── shelflife/
│       ├── model/
│       │   ├── PaymentMethod.java      # CASH, CARD, UPI, OTHER
│       │   ├── PaymentStatus.java      # PAID, PENDING, CANCELLED, REFUNDED
│       │   ├── Product.java            # Shared product model (Member 1)
│       │   ├── ProductBatch.java       # Shared batch model with isExpired()
│       │   ├── Sale.java               # Primary ownership: Member 3
│       │   ├── SaleItem.java           # Primary ownership: Member 3
│       │   └── WastageRecord.java      # Shared wastage model (Member 2/4)
│       ├── service/
│       │   ├── InventoryService.java   # Shared inventory interface
│       │   ├── InventoryServiceImpl.java
│       │   ├── SalesService.java       # Sales contract interface
│       │   ├── SalesServiceImpl.java   # FEFO allocation & commit engine
│       │   └── SaleException.java      # Checked sales exception
│       ├── storage/
│       │   ├── DataStore.java          # Shared storage interface
│       │   ├── FileManager.java        # Atomic CSV persistence engine
│       │   └── StorageException.java   # Persistence runtime exception
│       ├── ui/billing/
│       │   ├── BillingPanel.java       # Required Main Swing Panel
│       │   ├── CartItem.java           # In-memory cart staging model
│       │   ├── ReceiptDialog.java      # Customer receipt popup
│       │   └── SalesHistoryDialog.java # Transaction history browser
│       └── example/
│           └── BillingDemo.java        # Executable standalone demo app
├── test/
│   └── shelflife/test/
│       └── BillingAndSalesTest.java    # Automated 8-scenario test suite
├── run_demo.bat                        # 1-click batch launcher for GUI Demo
└── run_test.bat                        # 1-click batch launcher for Test Suite
```

---

## 3. Core Business Rules & Implementation Details

### A. In-Memory Cart Staging & Zero Side-Effects on Cancellation
* Items added to the cashier cart are stored in an in-memory `List<CartItem>` inside `BillingPanel`.
* **Zero Inventory Mutation:** Merely adding, editing, or removing items from the cart, or clearing the cart completely, causes zero modifications to persistent files or `ProductBatch` inventory.
* Inventory quantities are **only** deducted when the cashier confirms the sale and `SalesService.commitSale(...)` executes successfully.

### B. Strict Financial Accuracy with `BigDecimal`
* Per university guidelines, `double` and `float` are **never** used for financial calculations to prevent binary floating-point roundoff errors (such as `0.1 + 0.2 = 0.30000000000000004`).
* Line totals and grand totals use `BigDecimal` with `RoundingMode.HALF_UP` and a scale of 2 decimal places:
  $$\text{LineTotal} = \text{UnitPrice} \times \text{Quantity}$$
* Fractional prices (e.g., Rs. 19.99 x 3 = Rs. 59.97) compute with exact precision.

### C. First-Expiring, First-Out (FEFO) Batch Allocation Engine
1. **Exclusion of Expired Batches:**
   * Business Rule: A batch is expired if `batch.getExpiryDate().isBefore(LocalDate.now())`. A batch expiring **today is not expired**.
   * Any expired batch or batch with 0 quantity is strictly omitted from ordinary sales.
2. **Re-Validation at Confirmation:**
   * When checkout is confirmed, `SalesServiceImpl` recalculates the current sellable stock from scratch to prevent race conditions or stale displays.
3. **FEFO Sorting Order:**
   * Primary sort: Expiry date ascending (`b1.getExpiryDate().compareTo(b2.getExpiryDate())`).
   * Secondary sort (Tie-breaker): Batch ID ascending (`b1.getBatchId().compareTo(b2.getBatchId())`) for deterministic allocation.
4. **Multi-Batch Allocation:**
   * If a product order requires 14 units, and Batch 1 (expires in 2 days) has 10 units while Batch 2 (expires in 6 days) has 15 units:
     * Batch 1 is reduced by 10 (remaining: 0).
     * Batch 2 is reduced by 4 (remaining: 11).
     * Two distinct `SaleItem` records are saved: one for `B1` (qty: 10) and one for `B2` (qty: 4).

### D. Historical Price Immutability
* Each `SaleItem` records the actual unit price charged at the exact time of the transaction.
* If a product's price in the master catalog changes later (e.g., from Rs. 60.00 to Rs. 99.00), past sale items and customer receipts remain fixed at the original purchase price.

### E. Transactional Rollback & Error Recovery
* The atomic commit writes to `sales.csv`, `sale_items.csv`, and `batches.csv` through safe atomic files (`.tmp` and `.bak`) and writes a journal log.
* If persistence fails, `FileManager` restores the backup, in-memory caches are reloaded via `inventoryService.reload()`, and a `SaleException` is displayed to the cashier. The cart is not lost, and no partial records remain.

---

## 4. NetBeans Setup & Compile Instructions

### Option 1: Open Directly in Apache NetBeans
1. Launch **Apache NetBeans**.
2. Click **File -> Open Project...**
3. Navigate to `Member03/ShelfLife_Member3` and click **Open Project**.
4. Right-click the project and choose **Clean and Build**.
5. To run the demo: Right-click `BillingDemo.java` and select **Run File** (or press Shift+F6).
6. To run tests: Right-click `BillingAndSalesTest.java` and select **Run File**.

### Option 2: Command Line (Windows / Linux / macOS)
From the `ShelfLife_Member3` directory:

```powershell
# 1. Compile all source and test classes
New-Item -ItemType Directory -Path "bin" -Force
$files = (Get-ChildItem -Recurse -Filter *.java).FullName
javac -d bin $files

# 2. Run the automated test suite
java -cp bin shelflife.test.BillingAndSalesTest

# 3. Launch the interactive Swing GUI demo
java -cp bin shelflife.example.BillingDemo
```

Alternatively, simply double-click `run_demo.bat` or `run_test.bat`!

---

## 5. Automated Test Suite Results

The automated test runner (`BillingAndSalesTest.java`) validates all 8 mandatory university test scenarios:

```
======================================================================
   SHELFLIFE MEMBER 3: BILLING & SALES MANAGEMENT TEST RUNNER        
======================================================================
[RUNNING] 1. Successful Sale ... PASSED
[RUNNING] 2. Multi-Batch FEFO Allocation ... PASSED
[RUNNING] 3. Insufficient Stock Rejection ... PASSED
[RUNNING] 4. Expired Stock Exclusion ... PASSED
[RUNNING] 5. Cart Cancellation Zero Side-Effects ... PASSED
[RUNNING] 6. BigDecimal Rounding & Price Immutability ... PASSED
[RUNNING] 7. Repeated Confirmation & Duplicate Deduction Protection ... PASSED
[RUNNING] 8. Failed Storage Commit Rollback ... PASSED
======================================================================
TEST RESULTS: Passed: 8 | Failed: 0 | Total: 8
======================================================================
```

---

## 6. Sample Receipt Output

```
==============================================================
                  SHELFLIFE SUPERMARKET & POS                 
              Batch-Tracked Inventory & Billing               
                 Tel: +92-300-1234567 | Member 3              
==============================================================
Receipt No  : SALE-20261025-1001
Date & Time : 2026-10-25 12:40:22
Payment Mode: Cash
Status      : PAID
--------------------------------------------------------------
Item                 Batch        Qty      Price      Total
--------------------------------------------------------------
Fresh Whole Milk 1L  B-MILK-01     10      65.00     650.00
Fresh Whole Milk 1L  B-MILK-02      4      65.00     260.00
Whole Wheat Farm Bre B-BREAD-01     2      45.00      90.00
--------------------------------------------------------------
Subtotal:                                Rs.      1000.00
--------------------------------------------------------------
FINAL TOTAL PAID:                        Rs.      1000.00
==============================================================
             FEFO: Multi-Batch Allocation Verified            
         Thank you for choosing ShelfLife Supermarket!        
                 Please retain your receipt!                  
==============================================================
```

---

## 7. Integration Instructions for Member 5 (Main Application Window)

To integrate Member 3's Billing & Sales Management module into the primary tabbed GUI (`MainFrame`):

```java
// 1. Initialize or obtain shared DataStore and services
DataStore dataStore = new FileManager(Paths.get("data"));
InventoryService inventoryService = new InventoryServiceImpl(dataStore);
SalesService salesService = new SalesServiceImpl(inventoryService, dataStore);

// 2. Instantiate BillingPanel
BillingPanel billingPanel = new BillingPanel(inventoryService, salesService);

// 3. Add to MainFrame JTabbedPane
tabbedPane.addTab("Billing & Sales", billingPanel);

// 4. Synchronize data on tab change
tabbedPane.addChangeListener(e -> {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (tabbedPane.getComponentAt(selectedIndex) instanceof BillingPanel) {
        ((BillingPanel) tabbedPane.getComponentAt(selectedIndex)).refreshData();
    }
});
```
