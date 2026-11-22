# =========================================================================
# WorkNexa Multi-Stage Dockerfile
# =========================================================================

# Stage 1: Build JAR using Maven and OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application jar
COPY src src
RUN mvn clean package -DskipTests -B

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