@echo off
echo ======================================================================
echo    Running ShelfLife Member 3 Automated Test Suite
echo ======================================================================
if not exist "bin" mkdir bin
javac -d bin src\shelflife\model\*.java src\shelflife\storage\*.java src\shelflife\service\*.java src\shelflife\ui\billing\*.java test\shelflife\test\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
java -cp bin shelflife.test.BillingAndSalesTest
pause
