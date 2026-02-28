@echo off
chcp 65001 >nul
echo ========================================
echo   启动后端服务 (Spring Boot)
echo ========================================
cd /d "%~dp0backend"
call mvn spring-boot:run
pause
