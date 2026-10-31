@echo off
setlocal
mkdir "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" 2>nul
mkdir "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" 2>nul
mkdir "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\storage" 2>nul
mkdir "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui" 2>nul

echo Compiling model files...
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\Product.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\ProductBatch.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\Sale.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\SaleItem.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\WastageRecord.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\PaymentMethod.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\model" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\model\PaymentStatus.java"
if errorlevel 1 goto fail

echo Compiling service files...
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\DataStore.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\TransactionOperation.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\SaleException.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\InventoryService.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\InventoryServiceImpl.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\ExpiryService.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\ExpiryServiceImpl.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\SalesService.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\SalesServiceImpl.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\ReportService.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\service" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\service\ReportServiceImpl.java"
if errorlevel 1 goto fail

echo Compiling storage files...
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\storage" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\storage\CsvDataStore.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\storage" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\storage\StorageException.java"
if errorlevel 1 goto fail

echo Compiling UI files...
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui" -cp "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\ui\ShelfLifeApp.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui\billing" -cp "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\ui\billing\BillingPanel.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui\expiry" -cp "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\ui\expiry\ExpiryAlertsPanel.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui\inventory" -cp "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\ui\inventory\ProductInventoryPanel.java"
javac -target 8 -source 8 -d "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes\shelflife\ui\reports" -cp "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes" "E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src\shelflife\ui\reports\DashboardReportsPanel.java"
if errorlevel 1 goto fail

echo.
echo Compilation successful!
goto end

:fail
echo.
echo Compilation failed!
:end
endlocal