@echo off
chcp 65001 >nul
echo ========================================
echo   启动前端服务 (Vite Dev Server)
echo ========================================
cd /d "%~dp0frontend"
call npm run dev
pause
