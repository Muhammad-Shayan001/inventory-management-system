@echo off
echo ======================================================================
echo    Compiling and Running ShelfLife Member 3 Billing Demo
echo ======================================================================
if not exist "bin" mkdir bin
javac -d bin src\shelflife\model\*.java src\shelflife\storage\*.java src\shelflife\service\*.java src\shelflife\ui\billing\*.java src\shelflife\example\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
java -cp bin shelflife.example.BillingDemo
pause
