package th.mfu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    
    @Autowired
    private RTSPStreamService rtspStreamService;
    
    @Autowired
    private HLSStreamService hlsStreamService;

    @PostMapping("/rtsp/start")
    public ResponseEntity<String> startRTSPStream(
            @RequestParam(defaultValue = "8554") int port,
            @RequestParam String streamName) {
        //String inputUrl = "/tmp/videoplayback.mp4"; 
        String inputUrl = "C:/tmp/videoplayback.mp4"; 
        String rtspUrl = rtspStreamService.startStream(inputUrl, port, streamName);
        return ResponseEntity.ok(rtspUrl);
    }

    @PostMapping("/rtsp/stop")
    public ResponseEntity<Void> stopRTSPStream(@RequestParam String streamName) {
        rtspStreamService.stopStream(streamName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hls/start")
    public ResponseEntity<String> startHLSStream(
            @RequestParam(defaultValue = "8554") int rtspPort,
            @RequestParam String streamName) {
        // First get RTSP URL
        String rtspUrl = "rtsp://localhost:" + rtspPort + "/" + streamName;
        // Convert RTSP to HLS
        String hlsUrl = hlsStreamService.startHLSStream(rtspUrl, streamName);
        return ResponseEntity.ok(hlsUrl);
    }

    @PostMapping("/hls/stop")
    public ResponseEntity<Void> stopHLSStream(@RequestParam String streamName) {
        hlsStreamService.stopHLSStream(streamName);
        return ResponseEntity.ok().build();
    }

    // Serve HLS playlist and segments
    @GetMapping("/hls/{streamName}/stream.m3u8")
    public ResponseEntity<Resource> getHLSPlaylist(@PathVariable String streamName) {
        File playlistFile = new File("C:/tmp/hls/" + streamName + "/stream.m3u8");
        if (!playlistFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(playlistFile);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vnd.apple.mpegurl"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    @GetMapping("/hls/{streamName}/{segment}")
    public ResponseEntity<Resource> getHLSSegment(@PathVariable String streamName, @PathVariable String segment) {
        File segmentFile = new File("C:/tmp/hls/" + streamName + "/" + segment);
        if (!segmentFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(segmentFile);
        String contentType = segment.endsWith(".ts") ? "video/mp2t" : "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    // Check HLS stream status
    @GetMapping("/hls/{streamName}/status")
    public ResponseEntity<String> getHLSStreamStatus(@PathVariable String streamName) {
        File playlistFile = new File("C:/tmp/hls/" + streamName + "/stream.m3u8");
        File streamDir = new File("C:/tmp/hls/" + streamName);
        
        if (!streamDir.exists()) {
            return ResponseEntity.ok("Stream directory does not exist");
        }
        
        if (!playlistFile.exists()) {
            return ResponseEntity.ok("Playlist file does not exist yet");
        }
        
        // Check if there are any segment files
        File[] segmentFiles = streamDir.listFiles((dir, name) -> name.endsWith(".ts"));
        int segmentCount = segmentFiles != null ? segmentFiles.length : 0;
        
        return ResponseEntity.ok("Stream active - " + segmentCount + " segments found");
    }

    // Serve the HLS player HTML
    @GetMapping("/player")
    public ResponseEntity<Resource> getHLSPlayer() {
        Resource resource = new FileSystemResource("src/main/resources/static/hls-player.html");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}