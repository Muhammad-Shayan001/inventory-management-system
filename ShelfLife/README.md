# ShelfLife — Inventory, Expiry & Billing Management System

> **University Project** | Java 8 | Swing GUI | CSV Storage | PKR Currency | Offline Desktop App

---

## Overview

ShelfLife is a fully-featured retail inventory and Point-of-Sale (POS) management system built in pure Java 8 with a Swing GUI. It is designed for small retail shops in Pakistan and covers the complete lifecycle of a retail product — from supplier intake and batch tracking through to expiry monitoring, sales billing, customer credit (Khata/Udhaar), returns, and business reporting.

The system runs entirely offline, stores all data in local CSV files with `.bak` snapshot rollback for data safety, and requires no external libraries or frameworks beyond the standard JDK.

---

## Key Features

- **Role-Based Access Control** — Admin and Cashier roles with salted PBKDF2/SHA-256 password hashing
- **Product Catalog & Batch Management** — Multi-batch inventory with full CRUD, barcode/SKU support
- **FEFO Stock Allocation** — Automatically deducts from the earliest-expiring batch at checkout
- **Expiry Monitoring & Waste Tracking** — Near-expiry alerts, batch recall, clearance discount suggestions, waste-risk scoring
- **POS Billing** — Quick-sale buttons, split payments (Cash, Easypaisa, JazzCash, Card, Khata), hold/resume bills, below-cost warnings, 80mm thermal receipt printing
- **Customer Khata (Udhaar)** — Credit ledger with limit enforcement, payment tracking, and complete transaction history
- **Supplier Directory & Stock Intake** — Vendor management, purchase intake, supplier payables tracking
- **Returns & Refunds** — Against original sales with FEFO re-stocking
- **Reports & Dashboard** — Revenue trends, top-selling products, reorder lists, activity audit log
- **Data Safety** — Atomic multi-file transactions, automatic `.bak` snapshots, one-click ZIP backup/restore
- **CSV Import/Export** — Bulk product catalog import with preview; spreadsheet-compatible UTF-8 BOM export
- **Barcode Label Printing** — 24-up A4 barcode label sheets printed via Java 2D Graphics

---

## System Requirements

| Requirement | Minimum |
|---|---|
| Java Runtime | JDK 8 (Java SE 8) or later |
| Operating System | Windows 7+, macOS 10.12+, Linux (any with JVM) |
| RAM | 256 MB |
| Disk Space | 50 MB (data grows with usage) |
| Network | None — fully offline |
| External Libraries | **None** — pure standard Java library |

---

## How to Compile

```bat
cd "e:\Uni Learning\JAVA\2nd Sem\Shelf Life (Inventory Management System)\ShelfLife"
compile.bat
```

The script compiles all source files under `src/` into `build/classes/` and test classes into `build/test/classes/`.

---

## How to Run

```bat
cd "e:\Uni Learning\JAVA\2nd Sem\Shelf Life (Inventory Management System)\ShelfLife"
run.bat
```

Or manually:

```bat
java -cp "build\classes" shelflife.ui.ShelfLifeApp
```

---

## Default Login

On first launch ShelfLife checks for `~/ShelfLifeData/users.csv`. If no users exist, it bootstraps a default admin account:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Role | Administrator |

> **Change the password immediately** after first login via Settings → User Management.

You can also use **Sign Up** on the login screen to create additional accounts.

---

## Module Overview

| Module | Description | Primary Panel |
|---|---|---|
| Inventory & Batches | Product catalog, batch CRUD, barcode/SKU, stock levels | `ProductInventoryPanel` |
| Expiry Alerts | Near-expiry monitoring, wastage write-off, batch recall, traceability | `ExpiryAlertsPanel` |
| POS Billing | Cart management, FEFO checkout, split payment, receipt printing | `BillingPanel` |
| Returns & Refunds | Customer returns against original sale, refund arithmetic | `ReturnsPanel` |
| Suppliers | Vendor directory, stock intake receiving, payables tracking | `SuppliersPanel` |
| Reports | KPI dashboard, revenue trends, top products, reorder list | `DashboardReportsPanel` |
| Activity Log | Security audit trail — all CRUD and session events | `ActivityLogPanel` |
| Settings | Shop info, tax config, user management, database backup | `SettingsAndBackupPanel` |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 8 (SE) |
| GUI Framework | Java Swing + Java 2D |
| Data Storage | Plain CSV files (no database) |
| Currency Arithmetic | `java.math.BigDecimal` |
| Authentication | PBKDF2WithHmacSHA256 + random salt |
| Printing | `java.awt.print.Printable` / `PrinterJob` |
| Build | Custom `compile.bat` / `run.bat` shell scripts |
| Testing | Custom `SystemAuditTestSuite.java` (30+ test cases) |

---

## Project Structure

```
ShelfLife/
├── src/
│   └── shelflife/
│       ├── model/          # Domain entities (Product, Sale, Batch, Customer, …)
│       ├── service/        # Business logic interfaces + implementations
│       ├── storage/        # DataStore interface + CsvDataStore implementation
│       ├── ui/             # Swing panels, dialogs, theme
│       │   ├── billing/    # BillingPanel, ReceiptDialog, SplitPaymentDialog, …
│       │   ├── expiry/     # ExpiryAlertsPanel
│       │   ├── inventory/  # ProductInventoryPanel, SuppliersPanel
│       │   ├── reports/    # DashboardReportsPanel, ActivityLogPanel, …
│       │   └── theme/      # Theme, Styles, UITheme constants
│       └── util/           # CurrencyUtil, CsvExporter, BarcodeSheetPrinter, AppLogger
├── test/
│   └── shelflife/test/     # SystemAuditTestSuite (40+ automated tests)
├── data/                   # Sample CSV seed data
├── build/                  # Compiled class files (generated)
├── docs/                   # Additional reference docs
├── compile.bat             # Compile script
├── run.bat                 # Run script
├── test.bat                # Test runner
├── CONTRACT.md             # Architecture & data schema contract
└── README.md               # This file
```

---

## Authors

| Member | Responsibility |
|---|---|
| Member 1 | Product Catalog & Batch Inventory Management |
| Member 2 | Batch Expiry Date Monitoring & Alert System |
| Member 3 | POS Billing, FEFO Checkout & Receipt Printing |
| Member 4 | Reporting, KPI Dashboard & Data Analysis |
| Member 5 | System Integration, Core Architecture & UI Shell |

---

## License

University project — educational use only.
