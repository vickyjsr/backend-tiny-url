# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Install Gradle wrapper and project files first (better layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build the application
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install wget for HEALTHCHECK (optional: remove HEALTHCHECK below to drop this)
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser

# Copy the built JAR from builder (Spring Boot uses project name from settings.gradle)
COPY --from=builder /app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check for container orchestration (use /actuator/health if you add spring-boot-starter-actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/ 2>/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
