@echo off
echo Checking Video Gateway Services Status...
echo.

echo === Docker Compose Services ===
docker-compose ps

echo.
echo === MediaMTX Health Check ===
curl -s http://localhost:8888/v3/config/global/get >nul 2>&1
if %errorlevel% equ 0 (
    echo MediaMTX is running and healthy
) else (
    echo MediaMTX is not responding
)

echo.
echo === Port 8554 Check ===
netstat -an | findstr :8554
if %errorlevel% equ 0 (
    echo Port 8554 is listening
) else (
    echo Port 8554 is not listening
)

echo.
echo === Port 8080 Check ===
netstat -an | findstr :8080
if %errorlevel% equ 0 (
    echo Port 8080 is listening
) else (
    echo Port 8080 is not listening
)

echo.
echo === Container Logs (last 10 lines) ===
docker-compose logs --tail=10
