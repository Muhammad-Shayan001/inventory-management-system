# Data dictionary

ShelfLife keeps its data in plain CSV files, one file per table. This page is checked by the automated tests against the real files the program creates, so it cannot drift away from the code.

## How storage works

- **Location:** `~/ShelfLifeData/` (your user folder). Delete the folder to reset the app.
- **Format:** UTF-8, comma separated, values containing commas or quotes are wrapped in double quotes.
- **Safe writes:** changes that touch several files (for example a sale: sales, sale items, batches, payments) run inside one transaction. Before writing, each file is copied to a `.bak` snapshot; if anything fails, every file is restored from its snapshot.
- **Money:** amounts are decimal numbers in PKR with two decimals. Dates use `yyyy-MM-dd`, date and time use ISO format.
- **Stock is never typed in directly.** The stock of a product is the sum of the quantity of its batches. Sales, returns and wastage change batch quantities.

## Tables (25)

### `products.csv` - Product catalogue

One row per sellable product. Stock is not stored here; it is the sum of the product's batches.

```csv
productId,name,category,sellingPrice,lowStockThreshold,barcode,sku,active
```

### `batches.csv` - Stock batches

One row per received lot with its own expiry date and cost. Sales take stock from the batch that expires first (FEFO).

```csv
batchId,productId,quantity,expiryDate,costPrice,supplierId,receivedDate,recalled
```

### `sales.csv` - Sales (receipt header)

One row per completed sale. Totals are in PKR.

```csv
saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus,notes,cashierUsername,customerId,discountAmount,reprintCount
```

### `sale_items.csv` - Sale lines

The products and batches sold in each sale, with the cost price at the time of sale (used for profit).

```csv
saleId,productId,batchId,quantity,unitPrice,lineTotal,costPrice
```

### `sale_payments.csv` - Sale payments

One row per payment used on a sale (a sale can be paid with several methods, e.g. cash plus Easypaisa).

```csv
paymentLineId,saleId,paymentMethod,amount,referenceNote,timestamp
```

### `wastage.csv` - Wastage

Units written off from a batch (expired, damaged, spoiled).

```csv
wastageId,batchId,quantity,wastageDateTime,reason
```

### `returns.csv` - Returns (header)

One row per customer return and the refund given.

```csv
returnId,originalSaleId,returnDateTime,totalRefund,processedBy,reason
```

### `return_items.csv` - Return lines

Products returned; the disposition says whether stock went back to the shelf or was written off.

```csv
returnItemId,returnId,productId,productName,batchId,quantity,unitPrice,lineRefund,disposition
```

### `held_bills.csv` - Held bills (header)

Bills a cashier paused to serve another customer.

```csv
heldBillId,createdAt,cashierUsername,customerName,note,itemCount,estimatedTotal
```

### `held_bill_items.csv` - Held bill lines

Items inside a held bill.

```csv
heldItemId,heldBillId,productId,productName,quantity,unitPrice,discountAmount,batchId
```

### `customers.csv` - Customers (Khata)

Customers who may buy on credit, with a credit limit and current balance.

```csv
customerId,fullName,phoneNumber,address,creditLimit,currentBalance,active,createdAt
```

### `customer_ledger.csv` - Customer ledger

Every credit sale and payment for a customer, with a running balance.

```csv
entryId,customerId,timestamp,transactionType,referenceId,debit,credit,runningBalance,notes,cashierUsername
```

### `cashier_shifts.csv` - Cashier shifts

Opening float, cash sales and refunds, and the counted cash when a shift is closed.

```csv
shiftId,cashierUsername,openedAt,closedAt,openingFloat,cashSales,cashRefunds,expectedCash,actualCash,cashDifference,notes,status
```

### `price_overrides.csv` - Price overrides

Audit trail of manual price changes at the till, with the admin who approved them.

```csv
overrideId,timestamp,cashierUsername,adminApprover,productId,originalPrice,overriddenPrice,reason
```

### `suppliers.csv` - Suppliers

People and companies stock is bought from.

```csv
supplierId,name,contactPerson,phone,email,address,active
```

### `purchase_orders.csv` - Stock purchases

Stock received from suppliers, with unit cost, extra cost (e.g. transport) and how much was paid.

```csv
purchaseId,supplierId,supplierInvoiceNo,orderDate,receivedDate,productId,batchId,quantity,unitCost,additionalCost,totalAmount,paidAmount,paymentMethod,paymentAccount,expiryDate,status,createdBy,notes
```

### `supplier_payables.csv` - Supplier payables

Money still owed to suppliers for credit purchases.

```csv
payableId,supplierId,purchaseRef,invoiceDate,dueDate,totalAmount,paidAmount,balanceDue,status,notes
```

### `supplier_payments.csv` - Supplier payments

Payments made against a payable.

```csv
paymentId,payableId,supplierId,paymentDate,amount,paymentMethod,referenceNumber,notes,recordedBy
```

### `capital_transactions.csv` - Owner capital

Money the owner puts in or takes out. Not counted as sales revenue.

```csv
transactionId,type,amount,dateTime,investorName,paymentMethod,destinationAccount,description,createdBy
```

### `expenses.csv` - Expenses

Running costs such as rent and electricity.

```csv
expenseId,category,description,amount,expenseDate,createdAt,paymentMethod,paymentAccount,paidTo,referenceNumber,createdBy,notes
```

### `accounts.csv` - Cash and bank accounts

Where the shop's money is held (cash drawer, bank, wallets).

```csv
accountId,name,type,openingBalance,currentBalance,active
```

### `account_transactions.csv` - Account transactions

Every movement in or out of an account, with a running balance.

```csv
transactionId,accountId,dateTime,type,debit,credit,runningBalance,referenceId,description,createdBy
```

### `users.csv` - Users

Staff accounts. Passwords are stored as salted PBKDF2 hashes, never as plain text.

```csv
userId,username,passwordHash,salt,role,fullName,active,mustChangePassword
```

### `activity_logs.csv` - Activity log

Audit trail of important actions (who did what and when).

```csv
logId,timestamp,username,action,details,recordId
```

### `settings.csv` - Settings

Single-row shop settings.

```csv
shopName,currencyCode,currencySymbol,expiryWarningDays,defaultLowStockThreshold,taxRatePercent
```
