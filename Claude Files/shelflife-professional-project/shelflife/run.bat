@echo off
setlocal
cd /d "%~dp0"

if not exist "build\classes\shelflife\ui\ShelfLifeApp.class" (
    call compile.bat
    if errorlevel 1 exit /b 1
)

start "" javaw -cp build\classes shelflife.ui.ShelfLifeApp
endlocal
