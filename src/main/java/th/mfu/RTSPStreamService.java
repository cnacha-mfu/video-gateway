package th.mfu;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.*;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.avformat;
import javax.annotation.PostConstruct;

@Service
public class RTSPStreamService {
    private final ConcurrentHashMap<String, FFmpegFrameGrabber> activeStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FFmpegFrameRecorder> activeRecorders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize FFmpeg logging
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();
    }

    public String startStream(String inputUrl, int port, String streamName) {
        try {
            System.out.println("Starting stream from " + inputUrl);
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputUrl);
            grabber.start();

            // Use MediaMTX service name for Docker Compose network
            String mediamtxHost = System.getenv("MEDIAMTX_HOST");
            if (mediamtxHost == null) {
                mediamtxHost = "localhost"; // fallback for local development
            }
            String outputUrl = "rtsp://" + mediamtxHost + ":" + port + "/" + streamName;
            System.out.println("RTSPStreamService - Using MediaMTX host: " + mediamtxHost);
            System.out.println("RTSPStreamService - Output URL: " + outputUrl);
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputUrl,
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels()
                    );
            
            // Configure grabber for ultra-low latency
            grabber.setOption("fflags", "nobuffer+fastseek+genpts");  // reduce internal buffering
            grabber.setOption("flags", "low_delay");
            grabber.setOption("framedrop", "false");
            grabber.setOption("analyzeduration", "1000000");  // 1 second analysis
            grabber.setOption("probesize", "1000000");        // 1MB probe size
            grabber.setOption("sync", "ext");                 // External sync
            
            // Configure recorder for RTSP client with optimized settings
            recorder.setFormat("rtsp");
            recorder.setOption("rtsp_transport", "tcp");
            recorder.setOption("rtsp_flags", "prefer_tcp");
            recorder.setOption("muxdelay", "0.1");            // Minimal mux delay
            recorder.setOption("muxpreload", "0.1");          // Minimal mux preload
            
            // --- Video Configuration ---
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(grabber.getFrameRate()); // match original FPS
            recorder.setGopSize((int) grabber.getFrameRate()); // 1s keyframe
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");
            // Note: Removed profile setting as it's not compatible with OpenH264
            recorder.setVideoOption("crf", "23");             // Constant rate factor
            recorder.setVideoOption("maxrate", "2M");         // Max bitrate
            recorder.setVideoOption("bufsize", "4M");         // Buffer size
            recorder.setVideoOption("keyint_min", "30");      // Minimum keyframe interval
            recorder.setVideoOption("sc_threshold", "0");     // Disable scene change detection

            // --- Audio Configuration ---
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setAudioBitrate(128_000);
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioOption("aac_coder", "fast");     // Use fast AAC encoder
            recorder.setAudioOption("aac_latm", "0");         // Disable LATM
            recorder.setAudioOption("aac_mux_latm", "0");     // Disable LATM muxing

            recorder.start();

            activeStreams.put(streamName, grabber);
            activeRecorders.put(streamName, recorder);

            // Start streaming in a separate thread
            new Thread(() -> streamFramesRealTime(streamName)).start();

            return outputUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start RTSP stream", e);
        }
    }

     private void streamFramesRealTime(String streamName) {
        FFmpegFrameGrabber grabber = activeStreams.get(streamName);
        FFmpegFrameRecorder recorder = activeRecorders.get(streamName);

        try {
            long startTime = System.currentTimeMillis();
            Frame frame;
            int frameIndex = 0;
            double frameRate = grabber.getFrameRate();

            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    // Calculate the expected time for this frame
                    long waitTime = (long)((frameIndex / frameRate) * 1000) - (System.currentTimeMillis() - startTime);
                    if (waitTime > 0) Thread.sleep(waitTime);
                    frameIndex++;
                }
                recorder.record(frame);
            }
        } catch (Exception e) {
            stopStream(streamName);
        }
    }

    private void streamFrames(String streamName) {
        FFmpegFrameGrabber grabber = activeStreams.get(streamName);
        FFmpegFrameRecorder recorder = activeRecorders.get(streamName);

        try {
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }
        } catch (Exception e) {
            stopStream(streamName);
        }
    }

    public void stopStream(String streamName) {
        try {
            FFmpegFrameGrabber grabber = activeStreams.remove(streamName);
            FFmpegFrameRecorder recorder = activeRecorders.remove(streamName);

            if (grabber != null)
                grabber.stop();
            if (recorder != null)
                recorder.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop RTSP stream", e);
        }
    }
}
