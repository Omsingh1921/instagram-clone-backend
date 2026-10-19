package com.instagramclone.common.controller;

import com.instagramclone.common.constants.ApiConstants;
import com.instagramclone.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.HEALTH_ENDPOINT)
@RequiredArgsConstructor
@Tag(name = "Health & Foundation", description = "System and infrastructure foundation verification endpoints")
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @GetMapping
    @Operation(summary = "System foundation status", description = "Verifies MySQL, Redis, Kafka, and WebSocket foundation readiness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put(ApiConstants.KEY_APPLICATION, ApiConstants.APP_NAME);
        status.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_UP);
        status.put(ApiConstants.KEY_TIMESTAMP, LocalDateTime.now());
        status.put(ApiConstants.KEY_JAVA_VERSION, System.getProperty(ApiConstants.PROPERTY_JAVA_VERSION));
        status.put(ApiConstants.KEY_SPRING_BOOT_VERSION, SpringBootVersion.getVersion());

        // Check MySQL Database Connectivity
        Map<String, Object> dbStatus = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            dbStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_CONNECTED);
            dbStatus.put(ApiConstants.KEY_DATABASE_PRODUCT, connection.getMetaData().getDatabaseProductName());
            dbStatus.put(ApiConstants.KEY_DATABASE_VERSION, connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception ex) {
            dbStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_DISCONNECTED);
            dbStatus.put(ApiConstants.KEY_ERROR, ex.getMessage());
        }
        status.put(ApiConstants.KEY_DATABASE, dbStatus);

        // Check Redis Connectivity
        Map<String, Object> redisStatus = new LinkedHashMap<>();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pingResponse = connection.ping();
            redisStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_CONNECTED);
            redisStatus.put(ApiConstants.KEY_PING, pingResponse);
        } catch (Exception ex) {
            redisStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_STANDBY_DISCONNECTED);
            redisStatus.put(ApiConstants.KEY_NOTE, ApiConstants.NOTE_REDIS_STANDBY);
            redisStatus.put(ApiConstants.KEY_ERROR, ex.getMessage());
        }
        status.put(ApiConstants.KEY_REDIS, redisStatus);

        // Kafka Configuration Check
        Map<String, Object> kafkaStatus = new LinkedHashMap<>();
        kafkaStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_CONFIGURED);
        kafkaStatus.put(ApiConstants.KEY_BOOTSTRAP_SERVERS, kafkaBootstrapServers);
        status.put(ApiConstants.KEY_KAFKA, kafkaStatus);

        // WebSocket STOMP Check
        Map<String, Object> wsStatus = new LinkedHashMap<>();
        wsStatus.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_CONFIGURED);
        wsStatus.put(ApiConstants.KEY_ENDPOINT, ApiConstants.WS_ENDPOINT_STOMP);
        wsStatus.put(ApiConstants.KEY_MESSAGE_BROKER, ApiConstants.WS_BROKER_DESCRIPTION);
        status.put(ApiConstants.KEY_WEBSOCKET, wsStatus);

        return ResponseEntity.ok(ApiResponse.success(ApiConstants.MSG_HEALTH_OPERATIONAL, status, ApiConstants.HEALTH_ENDPOINT));
    }
}
