FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Add a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the JAR file
COPY target/*.jar app.jar

# Create necessary directories with correct permissions
RUN mkdir -p /app/logs /app/config && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Default port for clinic-core
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run with proper memory settings and GC configurations
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=200", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]