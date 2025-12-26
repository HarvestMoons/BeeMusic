@echo off
chcp 65001 >nul
echo === Docker镜像推送脚本 ===

:: --------------------------
:: 1. 登录 Docker Hub
:: --------------------------
echo.
echo 1. 登录Docker Hub...
docker login
if %errorlevel% neq 0 (
    echo 登录失败，请检查Docker是否运行
    pause
    exit /b 1
)

:: --------------------------
:: 2. 定义镜像变量
:: --------------------------
set backend_local=ice-spring:latest
set backend_remote=beecool/music-player:latest

set frontend_local=ice-frontend:latest
set frontend_remote=beecool/music-player-frontend:latest

:: --------------------------
:: 3. 推送后端镜像
:: --------------------------
echo.
echo 2. 推送后端镜像...
docker images | findstr "ice-spring" >nul
if %errorlevel% neq 0 (
    echo 错误: 本地后端镜像 %backend_local% 不存在
    echo 请先运行: docker-compose build backend
    pause
    exit /b 1
)

docker tag %backend_local% %backend_remote%
docker push %backend_remote%
if %errorlevel% equ 0 (
    echo ✅ 后端镜像推送成功: %backend_remote%
) else (
    echo ❌ 后端镜像推送失败
)

:: --------------------------
:: 4. 推送前端镜像
:: --------------------------
echo.
echo 3. 推送前端镜像...
docker images | findstr "ice-frontend" >nul
if %errorlevel% neq 0 (
    echo 错误: 本地前端镜像 %frontend_local% 不存在
    echo 请先运行: docker-compose build frontend
    pause
    exit /b 1
)

docker tag %frontend_local% %frontend_remote%
docker push %frontend_remote%
if %errorlevel% equ 0 (
    echo ✅ 前端镜像推送成功: %frontend_remote%
) else (
    echo ❌ 前端镜像推送失败
)

echo.
echo 脚本执行完成
