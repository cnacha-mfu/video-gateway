REM Build the optimized Docker image
echo Building Docker image...
docker build --build-arg BUILDKIT_INLINE_CACHE=1 -t video-gateway .

REM Stop and remove existing container if it exists
docker stop video-gateway 2>nul
docker rm video-gateway 2>nul

REM Run the container with necessary port mappings
echo Starting container...
docker run -d --name video-gateway ^
    -p 8080:8080 ^
    -v c:/tmp:/tmp ^
    video-gateway

echo Container started successfully!
echo Application available at: http://localhost:8080
echo RTSP port available at: 8554
