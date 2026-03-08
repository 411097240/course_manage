@echo off
echo ========================================
echo   Building Frontend (Production)
echo ========================================
cd /d "%~dp0frontend"
echo   Installing dependencies (npm install)...
call npm install
echo.
echo   Generating production build (npm run build)...
call npm run build
echo.
echo ========================================
echo   Build complete! Output: frontend/dist
echo ========================================
pause
