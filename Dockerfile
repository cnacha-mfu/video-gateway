# Build stage
FROM eclipse-temurin:18-jdk-jammy AS builder
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for better dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# Runtime stage - Use Ubuntu for FFmpeg compatibility
FROM eclipse-temurin:18-jre-jammy

# Install FFmpeg and required libraries with specific versions for stability
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    wget \
    gdb \
    strace \
    curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


# Verify FFmpeg installation and create symlinks for JavaCV compatibility
RUN ffmpeg -version && \
    ln -sf /usr/bin/ffmpeg /usr/local/bin/ffmpeg && \
    ln -sf /usr/bin/ffprobe /usr/local/bin/ffprobe

# Set system limits to prevent crashes
RUN echo "* soft nofile 65536" >> /etc/security/limits.conf && \
    echo "* hard nofile 65536" >> /etc/security/limits.conf && \
    echo "* soft nproc 32768" >> /etc/security/limits.conf && \
    echo "* hard nproc 32768" >> /etc/security/limits.conf

# Configure FFmpeg environment for stability
ENV FFMPEG_THREAD_QUEUE_SIZE=1024
ENV FFMPEG_LOGLEVEL=warning
ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash -m appuser

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create necessary directories for video processing and set ownership
RUN mkdir -p /tmp/hls /tmp/videos && \
    chmod 755 /tmp/hls /tmp/videos && \
    chown -R appuser:appgroup app.jar /tmp/hls /tmp/videos

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with memory optimization and crash prevention
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx2g", \
    "-Djava.library.path=/usr/lib/x86_64-linux-gnu", \
    "-Djava.awt.headless=true", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=UTC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dorg.bytedeco.javacpp.logger=slf4j", \
    "-Dorg.bytedeco.javacpp.logger.level=WARN", \
    "-jar", "app.jar"]