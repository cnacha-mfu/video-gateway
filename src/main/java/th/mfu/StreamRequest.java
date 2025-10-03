package th.mfu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamRequest {
    @JsonProperty("streamName")
    private String streamName;
    
    @JsonProperty("rtspUrl")
    private String rtspUrl;
    
    @JsonProperty("rtspPort")
    private Integer rtspPort;
    
    @JsonProperty("inputType")
    private String inputType; // "file" or "rtsp"
    
    // Default constructor
    public StreamRequest() {}
    
    // Constructor with parameters
    public StreamRequest(String streamName, String rtspUrl, Integer rtspPort, String inputType) {
        this.streamName = streamName;
        this.rtspUrl = rtspUrl;
        this.rtspPort = rtspPort;
        this.inputType = inputType;
    }
    
    // Getters and Setters
    public String getStreamName() {
        return streamName;
    }
    
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }
    
    public String getRtspUrl() {
        return rtspUrl;
    }
    
    public void setRtspUrl(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }
    
    public Integer getRtspPort() {
        return rtspPort;
    }
    
    public void setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
    }
    
    public String getInputType() {
        return inputType;
    }
    
    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
    
    @Override
    public String toString() {
        return "StreamRequest{" +
                "streamName='" + streamName + '\'' +
                ", rtspUrl='" + rtspUrl + '\'' +
                ", rtspPort=" + rtspPort +
                ", inputType='" + inputType + '\'' +
                '}';
    }
}
