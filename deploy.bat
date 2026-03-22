@echo off
chcp 65001 >nul
echo === Docker镜像推送脚本 ===

setlocal enabledelayedexpansion

if "%~1"=="" (
    echo.
    echo 用法: deploy.bat 提交信息
    echo 示例: deploy.bat 更新播放器样式和部署镜像
    pause
    exit /b 1
)

set commit_message=%*
set push_failed=0

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
    set push_failed=1
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
    set push_failed=1
)

if not "!push_failed!"=="0" (
    echo.
    echo 存在镜像推送失败，已跳过 Git 提交。
    pause
    exit /b 1
)

:: --------------------------
:: 5. 提交 Git 变更
:: --------------------------
echo.
echo 4. 提交Git变更...
git rev-parse --is-inside-work-tree >nul 2>&1
if !errorlevel! neq 0 (
    echo 当前目录不是 Git 仓库，已跳过 Git 提交
    pause
    exit /b 1
)

git add .
if !errorlevel! neq 0 (
    echo Git add 失败
    pause
    exit /b 1
)

git diff --cached --quiet
if !errorlevel! equ 0 (
    echo 没有可提交的变更，已跳过 Git 提交
    echo.
    echo 脚本执行完成
    pause
    exit /b 0
)

git commit -m "%commit_message%"
if !errorlevel! neq 0 (
    echo Git commit 失败
    pause
    exit /b 1
)

echo ✅ Git 提交成功: %commit_message%

echo.
echo 5. 推送Git远程仓库...
git push
if !errorlevel! neq 0 (
    echo Git push 失败
    pause
    exit /b 1
)

echo ✅ Git push 成功

echo.
echo 脚本执行完成
