@echo off
chcp 65001 >nul
echo ========================================
echo   同时启动前端和后端服务
echo ========================================
start "后端服务" cmd /c "%~dp0start-backend.bat"
timeout /t 5 /nobreak >nul
start "前端服务" cmd /c "%~dp0start-frontend.bat"
echo.
echo 后端和前端服务已在新窗口中启动。
echo 后端: http://localhost:8080
echo 前端: http://localhost:5173
pause
