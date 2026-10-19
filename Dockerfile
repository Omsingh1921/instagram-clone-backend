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

# --- Stage 2: Minimal Alpine Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Render default port is 10000
ENV PORT=10000
EXPOSE 10000

# Shell execution ensures ${PORT:-10000} is dynamically expanded at runtime
ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -Xmx384m -Xms128m -XX:MaxMetaspaceSize=160m -Dserver.port=${PORT:-10000} -jar app.jar"]
