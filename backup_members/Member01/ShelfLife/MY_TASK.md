# MASTER PROMPT — SHELFLIFE MEMBER 1: PRODUCT & INVENTORY MANAGEMENT

Act as a senior Java developer, Java Swing instructor, and module implementation specialist. I am Member 1 of a five-member university Object-Oriented Programming project named ShelfLife. Your responsibility is to implement ONLY the Product and Inventory Management module, with complete source code, validation, tests, and integration instructions.

## Project Documentation
Read the full project proposal at:
`docs/ShelfLife_Group_Project_Proposal.pdf`

## PROJECT TECHNOLOGY
- Java, preferably Java 8-compatible syntax.
- Java Swing GUI, Apache NetBeans.
- Local CSV persistence through the shared storage/service architecture.
- BigDecimal for money; LocalDate for expiry dates.
- No cloud services, paid APIs, Maven, Gradle, or unnecessary external libraries.

## MY ASSIGNED RESPONSIBILITIES
1. Create ProductInventoryPanel.java in package shelflife.ui.inventory.
2. Provide a professional Swing interface using JTable, input fields, buttons, search, confirmation dialogs, and clear error messages.
3. Add products with productId, name, category, sellingPrice, and lowStockThreshold.
4. Display and search products by ID or name.
5. Update product information with validation.
6. Delete a product only if it has no batch or historical records that depend on it.
7. Add and manage ProductBatch records containing batchId, productId, quantity, and expiryDate.
8. Reject duplicate IDs, missing required fields, invalid prices, negative quantities, invalid dates, and batches referencing nonexistent products.
9. Refresh the table after successful operations and after other modules change shared data.
10. Do not create a separate inventory list or separate CSV storage implementation.

## MANDATORY SHARED ARCHITECTURE
Use package shelflife.model for Product, ProductBatch, Sale, SaleItem, and WastageRecord. Use shelflife.storage.DataStore and shelflife.service.InventoryService as the shared contracts. The panel constructor must be exactly ProductInventoryPanel(InventoryService inventoryService). Include public void refreshData(). Do not duplicate shared model classes, FileManager, or service classes.

## IMPORTANT
Before coding, ask me to provide the team's approved shared model/service files or interface contract if they are not included. Do not invent conflicting method signatures. If the approved contract is available, follow it exactly. If a required method is missing, list the missing contract items before proposing a minimal coordinated change. Never silently change shared APIs.

## DELIVERABLES
1. Brief module design and dependency list.
2. Complete source code for every file you create, with exact package declarations and file paths.
3. Explanation of public methods and how the module calls InventoryService.
4. NetBeans setup and compile instructions.
5. Test cases and expected results for add, search, update, batch creation, duplicate IDs, invalid input, and protected deletion.
6. Integration instructions for Member 5.
7. A list of every file created and any shared files required.

## QUALITY RULES
No pseudocode, placeholder methods, unexplained ellipses, or omitted source files. Keep code beginner-friendly and commented where useful. Catch and display errors appropriately. Do not claim compilation or execution succeeded unless actually tested. Do not implement the entire ShelfLife application. Work only on my assigned module and preserve compatibility with the other four members.
