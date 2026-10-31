# ShelfLife Architectural & Data Contract

**Version:** 2.0  
**Target Environment:** Java 8+ (Pure Standard Library / Offline), Swing Desktop GUI, Windows 10/11  
**Persistence:** ACID Compliant Multi-File CSV Storage Engine with Transaction Rollback  
**Monetary Standard:** `java.math.BigDecimal` (PKR currency format)

---

## 1. Domain Entities & Schemas

### 1.1 Core Entities (Existing & Enhanced)
1. **`Product`**: `productId`, `barcode`, `sku`, `name`, `category`, `costPrice`, `sellingPrice`, `reorderThreshold`, `unit`, `active`
2. **`ProductBatch`**: `batchId`, `productId`, `supplierId`, `quantity`, `costPrice`, `expiryDate`, `receivedDate`, `recalled`
3. **`Supplier`**: `supplierId`, `name`, `contactPerson`, `phone`, `email`, `address`, `active`
4. **`Sale`**: `saleId`, `timestamp`, `cashierUsername`, `subtotal`, `discountAmount`, `taxAmount`, `totalAmount`, `paymentMethod`, `paymentStatus`, `notes`, `customerId`, `isDuplicateReceipt`
5. **`SaleItem`**: `saleItemId`, `saleId`, `productId`, `batchId`, `productName`, `quantity`, `unitPrice`, `discountAmount`, `totalPrice`
6. **`SaleReturn`**: `returnId`, `originalSaleId`, `timestamp`, `cashierUsername`, `refundAmount`, `refundMethod`, `reason`
7. **`SaleReturnItem`**: `returnItemId`, `returnId`, `productId`, `batchId`, `productName`, `quantity`, `refundUnitPrice`, `restockToInventory`, `totalRefund`
8. **`WastageRecord`**: `wastageId`, `productId`, `batchId`, `productName`, `quantity`, `unitCostPrice`, `totalLossAmount`, `reason`, `date`, `recordedBy`
9. **`User`**: `userId`, `username`, `passwordHash`, `salt`, `fullName`, `role` (`ADMIN`, `CASHIER`), `active`, `createdAt`
10. **`AppSettings`**: `storeName`, `storeAddress`, `storePhone`, `receiptFooter`, `expiryWarningDays`, `taxRatePercent`, `currencySymbol`, `adminDiscountThreshold`

### 1.2 New Advanced Retail Entities

#### A. Held Bills (`held_bills.csv` & `held_bill_items.csv`)
- **`held_bills.csv`**:
  `heldBillId,createdAt,cashierUsername,customerName,note,itemCount,estimatedTotal`
- **`held_bill_items.csv`**:
  `heldItemId,heldBillId,productId,productName,quantity,unitPrice,discountAmount,batchId`
*Purpose:* Allows cashiers to temporarily suspend active billing sessions without losing item state, restore them later when customer is ready, or discard them.

#### B. Split Payments (`sale_payments.csv`)
- **`sale_payments.csv`**:
  `paymentLineId,saleId,paymentMethod,amount,referenceNote,timestamp`
*Methods Supported:* `CASH`, `EASYPAISA`, `JAZZCASH`, `DEBIT_CARD`, `CREDIT_CARD`, `BANK_TRANSFER`, `CUSTOMER_CREDIT`  
*Rule:* Sum of payments must equal `totalAmount` of sale (plus optional change returned on cash portion).

#### C. Customer Credit Ledger / Udhaar (`customers.csv` & `customer_ledger.csv`)
- **`customers.csv`**:
  `customerId,fullName,phoneNumber,address,creditLimit,currentBalance,active,createdAt`
- **`customer_ledger.csv`**:
  `entryId,customerId,timestamp,transactionType,referenceId,debit,credit,runningBalance,notes,cashierUsername`
*Transaction Types:* `SALE_ON_CREDIT` (increases balance), `PAYMENT_RECEIVED` (decreases balance), `ADJUSTMENT`  
*Rule:* Cannot exceed `creditLimit` unless explicitly overridden. Append-only ledger ensures non-repudiation.

#### D. Supplier Payables Ledger (`supplier_payables.csv` & `supplier_payments.csv`)
- **`supplier_payables.csv`**:
  `payableId,supplierId,purchaseRef,invoiceDate,dueDate,totalAmount,paidAmount,balanceDue,status,notes`
  *(Status: `UNPAID`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`)*
- **`supplier_payments.csv`**:
  `paymentId,payableId,supplierId,paymentDate,amount,paymentMethod,referenceNumber,notes,recordedBy`
*Rule:* Tracks inventory acquired on terms. Automatic updates whenever supplier payments are logged.

#### E. Cashier Shifts & Cash Reconciliation (`cashier_shifts.csv`)
- **`cashier_shifts.csv`**:
  `shiftId,cashierUsername,openedAt,closedAt,openingFloat,cashSales,cashRefunds,expectedCash,actualCash,cashDifference,notes,status`
  *(Status: `OPEN`, `CLOSED`)*
*Reconciliation Formula:*  
$$\text{Expected Cash} = \text{Opening Float} + \text{Cash Sales} - \text{Cash Refunds}$$  
$$\text{Cash Difference} = \text{Actual Counted Cash} - \text{Expected Cash}$$

#### F. Price Override Audit Log (`price_overrides.csv`)
- **`price_overrides.csv`**:
  `overrideId,timestamp,cashierUsername,adminApprover,productId,originalPrice,overriddenPrice,reason`

#### G. System Diagnostics & Error Log (`logs/app_error.log`)
- Appended whenever uncaught or unexpected runtime errors occur.
- Standard format: `[ISO_TIMESTAMP] [LEVEL] [THREAD] [LOGGER] Message \n Stacktrace`
- Sanitized: Passwords, PINs, and sensitive credential tokens are scrubbed.

---

## 2. Business Logic & Mathematical Formulas

1. **FEFO Allocation:**
   Earliest expiry batch is drawn first (`expiryDate` ascending). If batch is empty or insufficient, next batch is consumed. Recalled batches are strictly excluded.
2. **Waste-Risk Score Formula:**
   $$\text{AvgDailySales} = \frac{\text{Units Sold in Window (30 Days)}}{\text{Days in Window (30)}}$$
   $$\text{DaysToSell} = \begin{cases} 9999 & \text{if AvgDailySales} = 0 \\ \frac{\text{Remaining Batch Units}}{\text{AvgDailySales}} & \text{otherwise} \end{cases}$$
   $$\text{DaysUntilExpiry} = \text{expiryDate} - \text{today}$$
   - **HIGH RISK:** $\text{DaysUntilExpiry} \le 7$ OR $\text{DaysToSell} > \text{DaysUntilExpiry} \times 1.5$
   - **MEDIUM RISK:** $\text{DaysToSell} > \text{DaysUntilExpiry}$
   - **LOW RISK:** $\text{DaysToSell} \le \text{DaysUntilExpiry}$
3. **Automatic Clearance Suggestion:**
   - $\le 7$ days to expiry: Suggest 30% discount
   - $8 - 14$ days to expiry: Suggest 20% discount
   - $15 - 30$ days to expiry: Suggest 10% discount
4. **Below-Cost Selling Protection:**
   If $(\text{Selling Price} - \text{Discount}) < \text{Batch Cost Price}$, POS flashes a visual warning and requires confirmation.
5. **Manager Discount Authorization:**
   If discount percentage exceeds `adminDiscountThreshold` (default 5.0%), an Admin credential dialog is required to proceed.

---

## 3. Storage & Concurrency Guarantees

1. **Transactional Integrity:** Write-ahead temporary file (`.tmp`), atomic replace via `renameTo()`, and rollback snapshots (`.bak`).
2. **Spreadsheet Compatibility:** CSV exports begin with UTF-8 Byte Order Mark (`\uFEFF`) and RFC 4180 standard escaping for seamless viewing in Microsoft Excel without corrupting currency or Urdu/English text.
3. **Schema Versioning:** Managed through `schema_version.txt`. If version increases, automatic migration scripts update columns safely.
