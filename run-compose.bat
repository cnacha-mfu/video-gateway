@echo off
echo Starting Video Gateway with Docker Compose...

REM Stop and remove existing containers
echo Stopping existing containers...
docker-compose down

REM Build and start services
echo Building and starting services...
docker-compose up --build -d

REM Show status
echo.
echo Services started successfully!
echo.
echo Application available at: http://localhost:8080
echo RTSP server available at: rtsp://localhost:8554
echo MediaMTX API available at: http://localhost:8888
echo.
echo To view logs:
echo   docker-compose logs -f
echo.
echo To stop services:
echo   docker-compose down
echo.
echo To check service status:
echo   docker-compose ps
