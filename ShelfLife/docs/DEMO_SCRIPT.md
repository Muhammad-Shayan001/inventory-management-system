# ShelfLife 10-Minute Project Presentation & Demonstration Script (`DEMO_SCRIPT.md`)

This guide provides a structured, timing-calibrated script for presenting the **ShelfLife** application to project evaluators, professors, or clients.

---

## Presentation Overview & Timing Breakdown

| Time Elapsed | Segment | Key Focus | Presenter Action |
| :---: | :--- | :--- | :--- |
| **00:00 – 01:30** | Introduction & Problem Statement | Retail Expiry Challenges & Architecture | Show login dialog & clean console launch |
| **01:30 – 03:30** | Cashier POS & Automated FEFO Billing | Barcode Scan, Cart, FEFO Deductions | Perform a sale; inspect batch splitting & receipt |
| **03:30 – 05:00** | Customer Returns & Refund Disposition | Restock vs Wastage Write-Off | Process a return; show stock restock & refund |
| **05:00 – 06:30** | Expiry Tracking & Wastage Management | Visual Alert Badges & Write-Off | Show expired batches & log wastage |
| **06:30 – 08:00** | Supplier Receiving & Reorder Planning | Vendor Intake & Low Stock Alerts | Receive new stock; view Reorder Planning list |
| **08:00 – 09:15** | Reports Dashboard & Financial Analytics | Dynamic Net Operating Profit in PKR | Review 7 KPI cards, Net Profit, CSV Export |
| **09:15 – 10:00** | Security, Audit Trail & Rollback Engine | Salted SHA-256, Audit Log, Database Backup | View audit trail; create instant backup archive |

---

## Minute-by-Minute Click Walkthrough

### [00:00 – 01:30] Introduction & Dual Role Authentication
- **Spoken Points**:
  > *"Respected professors, welcome to our presentation of ShelfLife — an offline Point-of-Sale, Expiry Tracking, and Inventory Management System designed for supermarkets and pharmacies. Retailers face millions in annual losses due to expired goods and haphazard inventory rotation. ShelfLife solves this through automated First-Expiry-First-Out (FEFO) batch allocation, robust returns disposition, and multi-file transactional persistence."*
- **Actions**:
  1. Open command prompt in `ShelfLife/` and launch `run.bat`.
  2. Point out the **Nimbus Look-and-Feel** desktop dialog.
  3. Type `cashier` / `cashier123` to show Role-Based Access Control (RBAC). Note how the sidebar only displays Billing, Returns, and Product Lookup.
  4. Click **Switch User / Logout**, then log in as `admin` / `admin123` to reveal all 9 administrative modules.

---

### [01:30 – 03:30] Cashier POS & Automated FEFO Stock Allocation
- **Spoken Points**:
  > *"Let's demonstrate a customer checkout. Notice that the cashier does not need to guess which batch to pull from the shelf. When we scan or select a product, our sales engine automatically queries all non-expired batches and prioritizes the earliest expiring stock first."*
- **Actions**:
  1. Click **Billing & Sales POS** in the sidebar.
  2. In the quick-scan box, scan or type a barcode (e.g. `896400000001` for Olpers Milk) or select from the dropdown.
  3. Add 4 units to the cart.
  4. Select **Payment Method**: Choose **JazzCash** or **Cash**.
  5. Click **Complete Sale**.
  6. The **Receipt Modal** pops up: point out the itemized breakdown, tax calculation, and uniform Pakistani Rupee formatting (`Rs. #,##0.00`).
  7. Explain that the sale was written atomically to `sales.csv` and `sale_items.csv`, and an archival `.txt` receipt was generated in `receipts/`.

---

### [03:30 – 05:00] Customer Returns & Dual Refund Disposition
- **Spoken Points**:
  > *"Retail operations require handling customer returns smoothly without corrupting inventory. ShelfLife supports partial returns with exact refund arithmetic in PKR and dual disposition: restocking healthy items versus writing off damaged goods."*
- **Actions**:
  1. Click **Returns & Refunds** in the sidebar.
  2. Copy the Sale ID from the receipt or click **Lookup Transaction** with the recent sale ID.
  3. Show that the system displays how many units were originally sold and verifies maximum allowable returns.
  4. Enter `1` unit to return.
  5. Select **Disposition: RESTOCK** (or **WASTAGE**):
     - Explain that choosing `RESTOCK` increases sellable batch inventory.
     - Choosing `WASTAGE` permanently writes off the damaged return to `wastage.csv` while still refunding the customer.
  6. Click **Process Return & Refund** and observe the confirmation dialog.

---

### [05:00 – 06:30] Expiry Alerts & Stock Wastage Write-Off
- **Spoken Points**:
  > *"Our Expiry Alerts panel acts as an early warning radar for store managers. Batches are color-coded based on days until expiration."*
- **Actions**:
  1. Click **Expiry Alerts & Wastage**.
  2. Point out the status badges:
     - **Red**: Expired batches (blocked from POS checkout).
     - **Orange**: Near-expiry batches (approaching threshold).
     - **Green**: Healthy inventory.
  3. Select an expired batch, choose reason `"Expired On Shelf"`, and click **Record Stock Wastage**.
  4. Point out how the batch quantity is reduced and the loss is logged.

---

### [06:30 – 08:00] Supplier Directory & Reorder Planning
- **Spoken Points**:
  > *"When stock falls below safety levels, the store must restock. Here is our Stock Reorder Planning module."*
- **Actions**:
  1. Click **Stock Reorder List**.
  2. Show that products with total stock $\le$ minimum threshold are automatically listed with current quantities, minimum thresholds, and linked supplier contacts.
  3. Click **Stock Intake & Suppliers** to demonstrate receiving fresh shipments into batch inventory with linked supplier ID and purchase cost price (PKR).

---

### [08:00 – 09:15] Executive KPI Dashboard & Dynamic Net Operating Profit
- **Spoken Points**:
  > *"Most basic student projects only display total sales. ShelfLife implements dynamic, true Net Operating Profit."*
- **Actions**:
  1. Click **Reports & Dashboard**.
  2. Walk through the 7 top KPI summary cards:
     - Total Products
     - Total Sellable Stock Units
     - Low Stock Items Count
     - Expired Batches Count
     - Today's Sales Revenue (PKR)
     - Total Wastage Losses (PKR)
     - **Net Operating Profit (PKR)**: Highlight the mathematical formula:
       $$\text{Net Profit} = \text{Revenue} - \text{COGS} - \text{Wastage} - \text{Refunds}$$
  3. Show the tabs: **Sales History**, **Stock Information**, **Wastage Records**, and **Returns History**.
  4. Click **Export Report Data (CSV)** on the Sales tab to demonstrate offline accounting export.

---

### [09:15 – 10:00] Security, Audit Trail & Database Backup
- **Spoken Points**:
  > *"Finally, let's examine the system administration, security, and data recovery features."*
- **Actions**:
  1. Click **Activity Audit Log**: Show the audit trail containing timestamps, usernames, action verbs, and record IDs for every sale, stock intake, and price edit.
  2. Click **Settings & Backup**:
     - Demonstrate changing store settings (shop name, tax percentage).
     - Click **Create Backup Archive**: Show the timestamped backup folder generated on disk.
  3. Conclude by mentioning the **30/30 automated regression test suite** executing cleanly in `test.bat`.
  4. Open the floor for questions from the evaluation committee.
