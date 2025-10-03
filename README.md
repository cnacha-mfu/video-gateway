# 🎥 Video Gateway - HLS Streaming Service

A Docker-based video streaming service that converts video files to HLS (HTTP Live Streaming) format with continuous streaming capabilities and automatic reconnection.

## 🚀 Features

- **HLS Streaming**: Convert video files to HLS format for web playback
- **RTSP Support**: RTSP streaming with MediaMTX server
- **Continuous Streaming**: Automatic reconnection and error recovery
- **Web Player**: Built-in HLS player with advanced features
- **Docker Compose**: Easy deployment with MediaMTX RTSP server
- **Cross-Platform**: Works on Windows, Linux, and macOS

## 📋 Prerequisites

- **Docker Desktop** (latest version)
- **Docker Compose** (included with Docker Desktop)
- **MP4 video file** (H.264 codec recommended)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <your-repository-url>
cd video-gateway
```

### 2. Prepare Your Video File

#### **Option A: Using Windows (Recommended)**
1. **Create the directory**:
   ```bash
   mkdir C:\tmp
   ```

2. **Copy your MP4 file** to the tmp directory:
   ```bash
   copy "path\to\your\video.mp4" "C:\tmp\videoplayback.mp4"
   ```

3. **Verify the file exists**:
   ```bash
   dir C:\tmp\videoplayback.mp4
   ```

#### **Option B: Using Linux/macOS**
1. **Create the directory**:
   ```bash
   mkdir -p /tmp
   ```

2. **Copy your MP4 file**:
   ```bash
   cp "path/to/your/video.mp4" "/tmp/videoplayback.mp4"
   ```

3. **Verify the file exists**:
   ```bash
   ls -la /tmp/videoplayback.mp4
   ```

### 3. Start the Services

#### **Windows**
```bash
run-compose.bat
```

#### **Linux/macOS**
```bash
docker-compose up --build -d
```

### 4. Verify Services are Running
```bash
check-status.bat
```

You should see:
- ✅ MediaMTX running on port 8554
- ✅ Video Gateway running on port 8080
- ✅ Port 8080 is listening
- ✅ Port 8554 is listening

## 🎬 Using the Video Gateway

### **📋 Important: Streaming Workflow**

The video gateway follows this workflow:
1. **Video File** → **RTSP Stream** → **HLS Stream** → **Web Player**
2. **RTSP must be started first** to create the source stream
3. **HLS converts** the RTSP stream for web playback
4. **Web player** displays the HLS stream

### **Method 1: Web Player (Recommended)**

1. **Open your browser** and navigate to:
   ```
   http://localhost:8080
   ```

2. **Enter a stream name** (e.g., "myStream", "test", "live")

3. **Start RTSP Stream First** (Important!):
   - Click "Start RTSP Stream" button
   - Wait for confirmation that RTSP stream is active
   - This creates the RTSP source that HLS will convert from

4. **Start HLS Stream**:
   - Click "Start HLS Stream"
   - Wait for confirmation - you'll see "HLS stream started successfully!"

5. **The player will automatically load** the stream after 3 seconds

### **Method 2: VLC Player**

1. **Start RTSP Stream First**:
   -  use API: `curl -X POST "http://localhost:8080/api/stream/rtsp/start?streamName=myStream"`

2. **Start the HLS stream** using the web player

3. **Open VLC Player**

4. **Go to**: Media → Open Network Stream (Ctrl+N)

5. **Enter the HLS URL**:
   ```
   http://localhost:8080/api/stream/hls/myStream/stream.m3u8
   ```
   (Replace "myStream" with your actual stream name)

6. **Click Play**

### **Method 3: Direct API Access**

#### **Start HLS Stream (Query Parameters)**
```bash
curl -X POST "http://localhost:8080/api/stream/hls/start?streamName=myStream&rtspPort=8554"
```

#### **Start HLS Stream (JSON with Custom RTSP URL)**
```bash
curl -X POST "http://localhost:8080/api/stream/hls/start" \
  -H "Content-Type: application/json" \
  -d '{
    "streamName": "myStream",
    "rtspUrl": "rtsp://localhost:8554/myStream",
    "inputType": "rtsp"
  }'
```

#### **Start HLS Stream (JSON with Default RTSP)**
```bash
curl -X POST "http://localhost:8080/api/stream/hls/start" \
  -H "Content-Type: application/json" \
  -d '{
    "streamName": "myStream",
    "rtspPort": 8554,
    "inputType": "rtsp"
  }'
```

#### **Check Stream Status**
```bash
curl "http://localhost:8080/api/stream/hls/myStream/status"
```

#### **Stop HLS Stream**
```bash
curl -X POST "http://localhost:8080/api/stream/hls/stop?streamName=myStream"
```

## 🔧 Configuration

### **Video File Requirements**
- **Format**: MP4 (recommended)
- **Codec**: H.264 video, AAC audio
- **Location**: `C:\tmp\videoplayback.mp4` (Windows) or `/tmp/videoplayback.mp4` (Linux/macOS)
- **Size**: No specific limit, but larger files may take longer to process

### **Stream Settings (Optimized for Fast Buffering)**
- **HLS Segment Duration**: 2 seconds (reduced from 4s)
- **HLS Playlist Size**: 3 segments (reduced from 5s)
- **Video Codec**: H.264 (default profile for compatibility)
- **Audio Codec**: AAC (if audio present)
- **Frame Rate**: 30 FPS
- **Preset**: ultrafast (for low latency)
- **GOP Size**: 2 seconds (60 frames)
- **Max Latency**: 4 seconds
- **Buffer Size**: 6 seconds (reduced from 30s)

### **Docker Configuration**
- **Memory Limit**: 4GB
- **CPU Limit**: 2 cores
- **File Descriptors**: 65536
- **Process Limit**: 32768

## 🌐 Network Access

### **Local Access**
- **Web Player**: `http://localhost:8080`
- **HLS Stream**: `http://localhost:8080/api/stream/hls/{streamName}/stream.m3u8`
- **RTSP Server**: `rtsp://localhost:8554`

### **Network Access (Other Devices)**
1. **Find your computer's IP address**:
   ```bash
   ipconfig  # Windows
   ifconfig  # Linux/macOS
   ```

2. **Use your IP address**:
   ```
   http://192.168.1.100:8080  # Replace with your IP
   ```

## 🔍 Troubleshooting

### **Common Issues**

#### **"Cannot find video file"**
- ✅ Verify file exists: `dir C:\tmp\videoplayback.mp4`
- ✅ Check file permissions
- ✅ Ensure file is named exactly `videoplayback.mp4`

#### **"Docker services not starting"**
- ✅ Check Docker Desktop is running
- ✅ Verify ports 8080 and 8554 are not in use
- ✅ Check Docker logs: `docker-compose logs -f`

#### **"Stream not loading in browser"**
- ✅ **Start RTSP stream first** before HLS stream
- ✅ Wait 3-5 seconds after starting each stream
- ✅ Check browser console for errors
- ✅ Verify stream is active: `check-status.bat`

#### **"HLS stream fails to start"**
- ✅ **Ensure RTSP stream is running first**
- ✅ Check RTSP stream status in web player
- ✅ Verify video file exists: `dir C:\tmp\videoplayback.mp4`
- ✅ Check Docker logs: `docker-compose logs -f video-gateway`

#### **"VLC cannot play stream"**
- ✅ Start stream in web player first
- ✅ Use correct URL format
- ✅ Check VLC version (update if needed)

### **Logs and Debugging**

#### **View Docker Logs**
```bash
docker-compose logs -f
```

#### **View Specific Service Logs**
```bash
docker-compose logs -f video-gateway
docker-compose logs -f mediamtx
```

#### **Check Container Status**
```bash
docker-compose ps
```

## 🛑 Stopping the Services

### **Stop All Services**
```bash
docker-compose down
```

### **Stop and Remove Everything**
```bash
docker-compose down -v --remove-orphans
```

## 📁 File Structure

```
video-gateway/
├── src/
│   ├── main/
│   │   ├── java/th/mfu/
│   │   │   ├── App.java
│   │   │   ├── StreamController.java
│   │   │   ├── HLSStreamService.java
│   │   │   └── RTSPStreamService.java
│   │   └── resources/
│   │       └── static/
│   │           └── hls-player.html
│   └── test/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── run-compose.bat
├── check-status.bat
└── README.md
```

## 🔄 Continuous Streaming Features

The HLS player includes advanced continuous streaming capabilities:

- **🔄 Automatic Reconnection**: Handles network interruptions
- **📊 Health Monitoring**: Checks stream status every 5 seconds
- **⚡ Ultra-Low Latency**: Optimized for live streaming (2-4 second latency)
- **🛡️ Error Recovery**: Multiple recovery strategies
- **📱 Cross-Platform**: Works on all modern browsers
- **🚀 Fast Buffering**: Optimized buffer settings for quick startup
- **📺 Live Sync**: Automatic synchronization to latest segments

## 🎯 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/stream/hls/start` | Start HLS stream (supports JSON and query params) |
| POST | `/api/stream/hls/stop` | Stop HLS stream |
| GET | `/api/stream/hls/{name}/stream.m3u8` | Get HLS playlist |
| GET | `/api/stream/hls/{name}/{segment}` | Get HLS segment |
| POST | `/api/stream/rtsp/start` | Start RTSP stream |
| POST | `/api/stream/rtsp/stop` | Stop RTSP stream |

### **JSON API Request Format**

#### **StreamRequest Object**
```json
{
  "streamName": "string (required)",
  "rtspUrl": "string (optional - custom RTSP URL)",
  "rtspPort": "integer (optional - default: 8554)",
  "inputType": "string (optional - 'rtsp' or 'file')"
}
```

#### **Examples**

**Custom RTSP URL:**
```json
{
  "streamName": "camera1",
  "rtspUrl": "rtsp://192.168.1.100:554/stream1",
  "inputType": "rtsp"
}
```

**Default RTSP with custom port:**
```json
{
  "streamName": "myStream",
  "rtspPort": 8554,
  "inputType": "rtsp"
}
```

**Minimal request:**
```json
{
  "streamName": "test"
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

If you encounter any issues:

1. **Check the troubleshooting section** above
2. **View the logs**: `docker-compose logs -f`
3. **Verify your setup** matches the prerequisites
4. **Create an issue** with detailed error information

## 🎉 Quick Start Summary

1. **Install Docker Desktop**
2. **Copy your MP4 file** to `C:\tmp\videoplayback.mp4`
3. **Run**: `run-compose.bat`
4. **Open**: `http://localhost:8080`
5. **Enter stream name** and click "Start RTSP Stream"
6. **Wait for RTSP confirmation**, then click "Start HLS Stream"
7. **Enjoy continuous streaming!**

---

**Happy Streaming! 🎥✨**
