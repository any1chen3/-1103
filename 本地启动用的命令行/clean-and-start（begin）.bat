@echo off
chcp 65001 > nul

echo ================================
echo   资产管理系统后端预处理ing
echo   Asset Management System
echo ================================
echo.

cd /d "D:\Projects\Asset Management Project\AssetManagementProject"
mvn clean install
pause