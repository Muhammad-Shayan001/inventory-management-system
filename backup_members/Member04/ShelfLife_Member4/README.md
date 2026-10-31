# ShelfLife - Member 4 Module (Storage, Dashboard & Reports)

This is the complete source code package for Member 4 of the ShelfLife OOP Group Project.

## Structure
- `src/shelflife/model/`: Shared domain models (`Product`, `ProductBatch`, `Sale`, `SaleItem`, `WastageRecord`).
- `src/shelflife/storage/`: Shared persistence system (`DataStore`, `FileManager`, `Csv`, `StorageException`).
- `src/shelflife/service/`: Reporting business logic (`ReportService`, `DefaultReportService`).
- `src/shelflife/ui/reports/`: Java Swing Dashboard & Reports Panel (`DashboardReportsPanel`).
- `src/shelflife/example/`: Runnable demo app (`DashboardDemo`).
- `data/`: CSV storage directory.

## How to Run
1. Open NetBeans or any Java IDE.
2. Create a project with package `shelflife`.
3. Copy the `src` and `data` folders into your project directory.
4. Run `shelflife.example.DashboardDemo.java`.
