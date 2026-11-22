# =========================================================================
# WorkNexa Multi-Stage Dockerfile
# =========================================================================

# Stage 1: Build JAR using Maven and OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Backup mirror of Maven Central, used only if the main repository rate-limits us (HTTP 429)
RUN printf '%s' '<settings><mirrors><mirror><id>google-central</id><mirrorOf>central</mirrorOf><url>https://maven-central.storage-download.googleapis.com/maven2/</url></mirror></mirrors></settings>' > /tmp/mirror-settings.xml

# Cache dependencies (retry a few times, then fall back to the mirror)
COPY pom.xml .
RUN for i in 1 2 3; do mvn -B dependency:go-offline && exit 0; echo "Maven download failed, retry $i"; sleep 20; done; \
    for i in 1 2 3; do mvn -B -s /tmp/mirror-settings.xml dependency:go-offline && exit 0; echo "Mirror download failed, retry $i"; sleep 20; done; \
    exit 1

# Copy source code and build application jar
COPY src src
RUN mvn -B clean package -DskipTests || mvn -B -s /tmp/mirror-settings.xml clean package -DskipTests

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S worknexa && adduser -S worknexa -G worknexa
USER worknexa:worknexa

# Copy fat jar from build stage
COPY --from=builder /workspace/target/worknexa-*.jar app.jar

# Expose HTTP port
EXPOSE 8080

# Memory-friendly defaults for small hosts (e.g. 512 MB free instances):
# heap is capped at ~65% of the container memory, leaving room for metaspace and threads.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError -XX:TieredStopAtLevel=1 -Xss512k"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]