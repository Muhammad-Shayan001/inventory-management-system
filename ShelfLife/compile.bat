@echo off
setlocal
echo ======================================================================
echo    Compiling ShelfLife - Java OOP Inventory and Billing System
echo ======================================================================

cd /d "%~dp0"

if not exist "build\classes" mkdir "build\classes"
if not exist "build\test\classes" mkdir "build\test\classes"

echo [1/2] Compiling main application source files...
javac -encoding UTF-8 -d "build\classes" src\shelflife\model\*.java src\shelflife\util\*.java src\shelflife\storage\*.java src\shelflife\service\*.java src\shelflife\ui\*.java src\shelflife\ui\theme\*.java src\shelflife\ui\inventory\*.java src\shelflife\ui\expiry\*.java src\shelflife\ui\billing\*.java src\shelflife\ui\reports\*.java src\shelflife\ui\chart\*.java src\shelflife\ui\reports\business\*.java
if %errorlevel% neq 0 (
    echo [ERROR] Application compilation failed!
    exit /b %errorlevel%
)

echo [2/2] Compiling audit and regression test suite...
javac -encoding UTF-8 -cp "build\classes" -d "build\test\classes" test\shelflife\test\*.java
if %errorlevel% neq 0 (
    echo [ERROR] Test compilation failed!
    exit /b %errorlevel%
)

echo.
echo [SUCCESS] ShelfLife compiled successfully!
echo Run the application using: run.bat
echo Execute tests using: test.bat
endlocal
