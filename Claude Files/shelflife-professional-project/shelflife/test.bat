@echo off
setlocal
cd /d "%~dp0"

call compile.bat
if errorlevel 1 exit /b 1

java -cp build\classes;build\test\classes shelflife.test.SystemAuditTestSuite
exit /b %errorlevel%
