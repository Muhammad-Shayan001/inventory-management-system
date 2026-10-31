@echo off
setlocal
echo ======================================================================
echo    Running ShelfLife Master System Audit and Test Suite
echo ======================================================================

cd /d "%~dp0"

echo Compiling latest code and test suite...
call compile.bat
if %errorlevel% neq 0 (
    echo [ERROR] Build failed. Aborting tests.
    exit /b %errorlevel%
)

java -cp "build\classes;build\test\classes" shelflife.test.SystemAuditTestSuite
endlocal
