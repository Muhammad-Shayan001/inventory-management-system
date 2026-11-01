@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo Compiling ShelfLife...
if exist build rmdir /s /q build
mkdir build\classes
mkdir build\test\classes

rem Collect every source file as a relative path (works even if the folder name contains spaces).
set "ROOT=%CD%\"
for /f "delims=" %%f in ('dir /s /b /a-d src\*.java') do (
    set "p=%%f"
    set "p=!p:%ROOT%=!"
    set "p=!p:\=/!"
    echo !p!>>build\sources.txt
)
for /f "delims=" %%f in ('dir /s /b /a-d test\*.java') do (
    set "p=%%f"
    set "p=!p:%ROOT%=!"
    set "p=!p:\=/!"
    echo !p!>>build\test-sources.txt
)

javac -encoding UTF-8 -Xlint:-options -d build\classes @build\sources.txt
if errorlevel 1 (
    echo [ERROR] Application compilation failed.
    exit /b 1
)

javac -encoding UTF-8 -Xlint:-options -cp build\classes -d build\test\classes @build\test-sources.txt
if errorlevel 1 (
    echo [ERROR] Test compilation failed.
    exit /b 1
)

echo [OK] Build finished. Run run.bat to start the app or test.bat to run the tests.
endlocal
