# SHELFLIFE — DATA DICTIONARY & PERSISTENCE SPECIFICATION

## 1. Storage Engine Overview
- **Storage Strategy:** Zero-dependency, offline, human-readable CSV flat files located in `~/ShelfLifeData/` (or local fallback `data/`).
- **Standard:** RFC-4180 with standard CSV quoting (commas and double quotes escaped via `""`), CRLF line endings, and UTF-8 encoding.
- **ACID Transaction Protocol:** Multi-file modifications execute under `DataStore.executeTransaction(Runnable)`. Before writing, snapshot `.bak` copies are created. If any IO error occurs during writes, all files are atomically restored to `.bak` snapshots.

---

## 2. Table Schemas & Data Types

### 2.1 `products.csv`
Defines master catalog goods sold in the store.
```csv
productId,name,category,sellingPrice,lowStockThreshold,active,barcode,sku
```
| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `productId` | `VARCHAR(32)` (PK) | Unique uppercase product code | `PROD-01` |
| `name` | `VARCHAR(120)` | Full trade name of the product | `Olpers Milk 1L` |
| `category` | `VARCHAR(60)` | Product category | `Dairy` |
| `sellingPrice` | `DECIMAL(12,2)` | Default retail price per unit in PKR | `280.00` |
| `lowStockThreshold` | `INT` | Minimum stock units triggering reorder alert | `15` |
| `active` | `BOOLEAN` | `true` if active, `false` if soft-deleted | `true` |
| `barcode` | `VARCHAR(60)` | EAN-13, UPC, or custom barcode string | `8964001234567` |
| `sku` | `VARCHAR(60)` | Stock Keeping Unit code | `SKU-DAIRY-001` |

### 2.2 `batches.csv`
Physical inventory lots with specific expiration dates and cost prices.
```csv
batchId,productId,quantity,expiryDate,costPrice,supplierId,receivedDate,recalled
```
| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `batchId` | `VARCHAR(32)` (PK) | Unique batch identification string | `B-OLPERS-101` |
| `productId` | `VARCHAR(32)` (FK) | Reference to `products.productId` | `PROD-01` |
| `quantity` | `INT` | Available sellable physical units | `50` |
| `expiryDate` | `DATE (YYYY-MM-DD)` | Expiration date | `2026-12-31` |
| `costPrice` | `DECIMAL(12,2)` | Landed unit purchase cost in PKR | `240.00` |
| `supplierId` | `VARCHAR(32)` (FK) | Reference to `suppliers.supplierId` | `SUP-ENGRO` |
| `receivedDate`| `DATE (YYYY-MM-DD)` | Date goods were received into warehouse | `2026-10-15` |
| `recalled` | `BOOLEAN` | Flag blocking batch from checkout sale | `false` |

### 2.3 `sales.csv`
Header record for finalized point-of-sale customer transactions.
```csv
saleId,saleDateTime,totalAmount,discountAmount,taxAmount,cashTendered,changeDue,paymentMethod,status,cashierUsername,customerId
```

### 2.4 `sale_items.csv`
Individual line items deducted during checkout.
```csv
saleItemId,saleId,productId,productName,batchId,quantity,unitPrice,costPrice,lineTotal
```

### 2.5 `suppliers.csv`
Vendor company contacts and delivery terms.
```csv
supplierId,name,contactPerson,phone,email,address,active
```

### 2.6 `purchase_orders.csv`
Commercial procurement orders and stock-in intake records.
```csv
orderId,supplierId,orderDate,status,totalBaseCost,freightCharges,totalLandedCost,notes
```

### 2.7 `supplier_payables.csv`
Credit obligations owed to suppliers for received stock shipments.
```csv
payableId,supplierId,purchaseRef,invoiceDate,dueDate,totalAmount,paidAmount,balanceDue,status,notes
```

### 2.8 `supplier_payments.csv`
Settlement payments made to clear supplier payables.
```csv
paymentId,payableId,paymentDate,amount,method,referenceNumber,notes,recordedBy
```

### 2.9 `capital_transactions.csv`
Owner equity contributions and personal drawings.
```csv
transactionId,dateTime,type,amount,accountId,description,recordedBy
```

### 2.10 `expenses.csv`
Indirect operating overhead expenses.
```csv
expenseId,dateTime,category,amount,description,paidVia,payee,recordedBy
```

### 2.11 `accounts.csv`
Multi-account cash and bank drawer master.
```csv
accountId,name,type,currentBalance,accountNumber,bankName,active
```

### 2.12 `account_transactions.csv`
Immutable transaction ledger for cash and bank accounts.
```csv
txnId,accountId,dateTime,type,amount,balanceAfter,referenceType,referenceId,description
```

### 2.13 `customers.csv`
Customer Khata accounts and credit limits.
```csv
customerId,fullName,phone,address,creditLimit,currentBalance,active,notes
```

### 2.14 `customer_ledger.csv`
Debit and credit entries tracking customer Khata balances.
```csv
entryId,customerId,dateTime,entryType,referenceId,debitAmount,creditAmount,runningBalance,notes,recordedBy
```

### 2.15 `cashier_shifts.csv`
Cashier register shifts and cash drawer reconciliation.
```csv
shiftId,cashierUsername,openedAt,closedAt,openingFloat,cashSales,cashRefunds,expectedCash,actualCashCounted,discrepancy,notes,status
```

### 2.16 `held_bills.csv` & `held_bill_items.csv`
Parked POS customer shopping carts.
```csv
billId,heldAt,cashierUsername,customerName,customerPhone,notes
billItemId,billId,productId,productName,quantity,unitPrice
```

### 2.17 `wastage.csv`
Damaged, spoiled, or expired stock written off.
```csv
wastageId,batchId,quantity,wastageDateTime,reason
```

### 2.18 `sale_returns.csv`
Customer return and refund records.
```csv
returnId,originalSaleId,returnDateTime,refundAmount,refundMethod,reason,processedBy
```

### 2.19 `users.csv`
Salted PBKDF2/SHA-256 hashed user credentials and role authorizations.
```csv
userId,username,passwordHash,salt,role,fullName,active
```

### 2.20 `activity_log.csv`
Security and operational audit trail.
```csv
logId,timestamp,username,action,details,recordId
```

### 2.21 `settings.csv`
Store configuration, tax rate, and warning thresholds.
```csv
key,value
```
