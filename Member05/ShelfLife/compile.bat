@echo off
set JAVA_HOME=
set DIR_PATH="E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\src"
set OUT_DIR="E:\Uni Learning\Java\2nd Sem\Shelf Life (Inventory Management System)\Member05\ShelfLife\build\classes"

mkdir %OUT_DIR%

echo Compiling model classes...
javac %DIR_PATH%\shelflife\model\Product.java %DIR_PATH%\shelflife\model\ProductBatch.java %DIR_PATH%\shelflife\model\Sale.java %DIR_PATH%\shelflife\model\SaleItem.java %DIR_PATH%\shelflife\model\WastageRecord.java %DIR_PATH%\shelflife\model\PaymentMethod.java %DIR_PATH%\shelflife\model\PaymentStatus.java -target 8 -source 8 -d %OUT_DIR%
if errorlevel 1 goto fail

echo Compiling service classes...
javac %DIR_PATH%\shelflife\service\DataStore.java %DIR_PATH%\shelflife\service\TransactionOperation.java %DIR_PATH%\shelflife\service\SaleException.java %DIR_PATH%\shelflife\service\InventoryService.java %DIR_PATH%\shelflife\service\InventoryServiceImpl.java %DIR_PATH%\shelflife\service\ExpiryService.java %DIR_PATH%\shelflife\service\ExpiryServiceImpl.java %DIR_PATH%\shelflife\service\SalesService.java %DIR_PATH%\shelflife\service\SalesServiceImpl.java %DIR_PATH%\shelflife\service\ReportService.java %DIR_PATH%\shelflife\service\ReportServiceImpl.java -target 8 -source 8 -d %OUT_DIR%
if errorlevel 1 goto fail

echo Compiling UI classes...
javac %DIR_PATH%\shelflife\ui\ShelfLifeApp.java %DIR_PATH%\shelflife\ui\inventory\ProductInventoryPanel.java %DIR_PATH%\shelflife\ui\expiry\ExpiryAlertsPanel.java %DIR_PATH%\shelflife\ui\billing\BillingPanel.java %DIR_PATH%\shelflife\ui\reports\DashboardReportsPanel.java -target 8 -source 8 -d %OUT_DIR% -cp %OUT_DIR%
if errorlevel 1 goto fail

echo Compilation successful!
goto end

:fail
echo Compilation failed!
:end