# Make ShelfLife compile, secure and professional

## Why
The project did not compile (139 errors in new chart and dashboard files), the README claimed PBKDF2 while the code used one round of SHA-256, the data dictionary was wrong for 17 of 20 tables, and the repository was full of generated and duplicated files (1,018 files, 346 `.class` files, 220 files in `backup_members/`).

## What changed
**It builds and works**
- Rewrote the three chart classes (shared base class, no NullPointerExceptions, tooltips, PNG export)
- Added real analytics to `ReportService` (daily sales, top products, payment mix, stock health, wastage, hourly sales, monthly profit, stock value, dead stock)
- Replaced the placeholder `BusinessOverviewPanel` (hard-coded numbers, fake exports, UI updated from a background thread) with a data-driven dashboard, wired into the administrator sidebar

**Security**
- `PasswordUtil` now really uses PBKDF2WithHmacSHA256 (120,000 iterations, constant-time compare)
- Old SHA-256 accounts still sign in and are upgraded automatically at login
- Default `admin` / `cashier` accounts must choose a new password on first sign-in

**Features that existed but were not connected**
- Print button on receipts, Manage Users from Settings, CSV product import with preview, `AccessControl` used for role checks
- Removed `MessagePrinter` (duplicate of the receipt text) and `AppLogger` (unused)

**Repository**
- Removed committed `.class` files, `backup_members/`, `Member01-05/`, the stale root `ui/` copy, temp files and committed runtime data; added `.gitignore`
- Moved the project from `ShelfLife/` to the repository root
- New `compile` / `run` / `test` scripts for Windows and Linux/macOS (the old `compile.bat` missed new packages)
- GitHub Actions workflow: builds and tests on Java 8 and 17

**Docs and tests**
- New README, `docs/ARCHITECTURE.md` (Mermaid diagrams), `docs/DATA_DICTIONARY.md` regenerated from the real CSV headers
- Tests grew from 40 to 44 scenarios; test 44 fails if the data dictionary and the code ever disagree
- Screenshots regenerated from a realistic 90-day demo shop (`test/shelflife/test/DemoShop.java`)

## Nothing is lost
Everything removed is still in Git history and in the `before-cleanup` tag, including each team member's original folder.

## How it was checked
Fresh clone, `./compile.sh`, `xvfb-run ./test.sh`: 44 of 44 scenarios pass. Also compiled with `--release 8`.

## Left for later
- `ThermalReceiptRenderer` (80 mm receipts) is not connected to the screens yet
- Price-override logging exists in the service but the till does not call it
- Choose a licence
