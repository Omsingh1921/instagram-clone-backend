# ============================================================
# INSTAGRAM CLONE BACKEND — MULTI-STAGE DOCKERFILE (RENDER / PROD)
# ============================================================

# --- Stage 1: Build JAR with Maven and OpenJDK 21 ---
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Minimal Distroless / Alpine Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Render dynamic port binding (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

# SerialGC and memory caps optimized for Render 512MB free tier
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx384m", "-Xms128m", "-XX:MaxMetaspaceSize=160m", "-jar", "app.jar"]
