REM Build the optimized Docker image
echo Building Docker image...
docker build --build-arg BUILDKIT_INLINE_CACHE=1 -t video-gateway .

REM Stop and remove existing container if it exists
docker stop video-gateway 2>nul
docker rm video-gateway 2>nul

REM Run the container with necessary port mappings and memory limits
echo Starting container...
docker run -d --name video-gateway ^
    -p 8080:8080 ^
    -v c:/tmp:/tmp ^
    -v c:/tmp/hls:/tmp/hls ^
    -v c:/tmp/videos:/tmp/videos ^
    --memory=4g ^
    --memory-swap=4g ^
    --cpus=2 ^
    --ulimit nofile=65536:65536 ^
    --ulimit nproc=32768:32768 ^
    --security-opt seccomp=unconfined ^
    --cap-add=SYS_PTRACE ^
    video-gateway

echo Container started successfully!
echo Application available at: http://localhost:8080
echo RTSP port available at: 8554
