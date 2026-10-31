@echo off
setlocal
echo Launching ShelfLife Desktop Application...
cd /d "%~dp0"

if not exist "build\classes\shelflife\ui\ShelfLifeApp.class" (
    echo Binaries not found. Running compilation first...
    call compile.bat
    if %errorlevel% neq 0 exit /b %errorlevel%
)

start javaw -cp "build\classes" shelflife.ui.ShelfLifeApp
endlocal
