package th.mfu;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.logging.Logger;

@Service
public class HLSStreamService {
    private static final Logger logger = Logger.getLogger(HLSStreamService.class.getName());
    
    public String startHLSStream(String rtspUrl, String streamName) {
        try {
            logger.info("Starting HLS stream for: " + rtspUrl + " with stream name: " + streamName);
            
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setOption("stimeout", "2000000");
            grabber.start();
            logger.info("RTSP grabber started successfully");
            logger.info("Video dimensions: " + grabber.getImageWidth() + "x" + grabber.getImageHeight());
            logger.info("Audio channels: " + grabber.getAudioChannels());
            logger.info("Sample rate: " + grabber.getSampleRate());

            // make sure directory exists
            File outputDir = new File("/tmp/hls/" + streamName);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                logger.info("Created output directory: " + outputDir.getAbsolutePath() + " - Success: " + created);
            } else {
                logger.info("Output directory already exists: " + outputDir.getAbsolutePath());
            }
            
            String hlsOutput = outputDir.getAbsolutePath() + "/stream.m3u8";
            logger.info("HLS output path: " + hlsOutput);

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(hlsOutput,
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels());

            // Optimized HLS and encoding settings for faster buffering
            recorder.setFormat("hls");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            
            // Only set audio codec if there are audio channels
            if (grabber.getAudioChannels() > 0) {
                logger.info("Configuring audio: channels=" + grabber.getAudioChannels() + ", sampleRate=" + grabber.getSampleRate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioBitrate(128000);
            } else {
                logger.info("No audio channels detected, skipping audio configuration");
            }
            
            // Optimized HLS settings for low latency and fast buffering
            recorder.setOption("hls_time", "2");                    // Shorter segments (2 seconds)
            recorder.setOption("hls_list_size", "3");               // Smaller playlist (3 segments)
            recorder.setOption("hls_flags", "delete_segments+append_list+independent_segments");
            recorder.setOption("hls_segment_type", "mpegts");       // Use MPEG-TS for better compatibility
            recorder.setOption("hls_allow_cache", "0");             // Disable caching for live streams
            
            // Video encoding optimizations for speed
            recorder.setOption("preset", "ultrafast");              // Fastest encoding preset
            recorder.setOption("tune", "zerolatency");              // Zero latency tuning
            // Note: Removed profile setting as it's not compatible with OpenH264
            recorder.setOption("crf", "23");                        // Constant rate factor for quality
            recorder.setOption("maxrate", "2M");                    // Maximum bitrate
            recorder.setOption("bufsize", "4M");                    // Buffer size
            
            // Frame rate and GOP settings
            recorder.setFrameRate(30);
            recorder.setGopSize(60);                                // 2-second GOP (30fps * 2s)
            
            // Additional low-latency options
            recorder.setOption("fflags", "+genpts+igndts");         // Generate PTS, ignore DTS
            recorder.setOption("avoid_negative_ts", "make_zero");   // Avoid negative timestamps
            recorder.setOption("fps_mode", "cfr");                  // Constant frame rate
            recorder.start();
            logger.info("HLS recorder started successfully");

            // Start streaming in a separate thread
            new Thread(() -> streamToHLS(grabber, recorder, streamName)).start();
            logger.info("Streaming thread started");

            // Return HTTP URL instead of file path
            String httpUrl = "http://localhost:8080/api/stream/hls/" + streamName + "/stream.m3u8";
            logger.info("Returning HLS HTTP URL: " + httpUrl);
            return httpUrl;
        } catch (Exception e) {
            logger.severe("Failed to start HLS stream: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start HLS stream", e);
        }
    }

    private void streamToHLS(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder, String streamName) {
        try {
            logger.info("Starting to stream frames to HLS for: " + streamName);
            Frame frame;
            int frameCount = 0;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
                frameCount++;
                if (frameCount % 100 == 0) {
                    logger.info("Processed " + frameCount + " frames for stream: " + streamName);
                }
            }
            logger.info("Finished streaming " + frameCount + " frames for: " + streamName);
        } catch (Exception e) {
            logger.severe("Error during HLS streaming for " + streamName + ": " + e.getMessage());
            e.printStackTrace();
            stopHLSStream(streamName);
        } finally {
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                logger.warning("Error cleaning up resources: " + e.getMessage());
            }
        }
    }

    public void stopHLSStream(String streamName) {
        // Clean up HLS segments
        File hlsDir = new File("/tmp/hls/" + streamName);
        if (hlsDir.exists()) {
            File[] files = hlsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            hlsDir.delete();
        }
    }
}
