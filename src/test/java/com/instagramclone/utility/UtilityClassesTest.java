package com.instagramclone.utility;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.ApiResponseBuilder;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.response.PaginationResponseBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilityClassesTest {

    // --- UUIDUtility Tests ---
    @Test
    @DisplayName("UUIDUtility: correctly parses valid UUID and throws on invalid")
    void testUUIDUtility() {
        UUID original = UUID.randomUUID();
        UUID parsed = UUIDUtility.parseOrThrow(original.toString(), "testId");
        assertThat(parsed).isEqualTo(original);

        assertThat(UUIDUtility.isValid(original.toString())).isTrue();
        assertThat(UUIDUtility.isValid("invalid-uuid-string")).isFalse();
        assertThat(UUIDUtility.isValid(null)).isFalse();

        assertThatThrownBy(() -> UUIDUtility.parseOrThrow("not-a-uuid", "testId"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> UUIDUtility.parseOrThrow(null, "testId"))
                .isInstanceOf(BadRequestException.class);

        assertThat(UUIDUtility.parseOptional(original.toString())).contains(original);
        assertThat(UUIDUtility.parseOptional("invalid")).isEmpty();
    }

    // --- DateTimeUtility Tests ---
    @Test
    @DisplayName("DateTimeUtility: ISO formatting and expiration checks")
    void testDateTimeUtility() {
        LocalDateTime now = DateTimeUtility.nowUtc();
        assertThat(now).isNotNull();

        String iso = DateTimeUtility.formatIso(now);
        assertThat(iso).isNotEmpty();

        assertThat(DateTimeUtility.isExpired((LocalDateTime) null)).isTrue();
        assertThat(DateTimeUtility.isInstantExpired(null)).isTrue();
        assertThat(DateTimeUtility.isExpired(now.minusHours(1))).isTrue();
        assertThat(DateTimeUtility.isExpired(now.plusHours(1))).isFalse();
    }

    // --- PaginationUtility Tests ---
    @Test
    @DisplayName("PaginationUtility: clamps page size within boundaries")
    void testPaginationUtility() {
        Pageable defaultPageable = PaginationUtility.createPageable(null, null);
        assertThat(defaultPageable.getPageNumber()).isEqualTo(PaginationUtility.DEFAULT_PAGE);
        assertThat(defaultPageable.getPageSize()).isEqualTo(PaginationUtility.DEFAULT_SIZE);

        Pageable cappedPageable = PaginationUtility.createPageable(2, 500);
        assertThat(cappedPageable.getPageNumber()).isEqualTo(2);
        assertThat(cappedPageable.getPageSize()).isEqualTo(PaginationUtility.MAX_SIZE);

        Pageable clamped = PaginationUtility.clamp(PageRequest.of(0, 999));
        assertThat(clamped.getPageSize()).isEqualTo(PaginationUtility.MAX_SIZE);
    }

    // --- FileUtility Tests ---
    @Test
    @DisplayName("FileUtility: validates allowed file extensions and sizes")
    void testFileUtility() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile validImage = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", jpegBytes
        );
        FileUtility.validateFile(validImage, false);

        MockMultipartFile invalidType = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", new byte[1024]
        );
        assertThatThrownBy(() -> FileUtility.validateFile(invalidType, false))
                .isInstanceOf(BadRequestException.class);

        byte[] mp4Bytes = new byte[]{0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0};
        MockMultipartFile validVideo = new MockMultipartFile(
                "video", "clip.mp4", "video/mp4", mp4Bytes
        );
        FileUtility.validateFile(validVideo, true);

        String ext = FileUtility.extractExtension("my_avatar.PNG");
        assertThat(ext).isEqualTo("png");

        String uniqueName = FileUtility.generateUniqueFilename("photo.jpg");
        assertThat(uniqueName).endsWith(".jpg");
    }

    // --- ApiUtility Tests ---
    @Test
    @DisplayName("ApiUtility: standardizes ResponseEntity creation")
    void testApiUtility() {
        ResponseEntity<ApiResponse<String>> okResponse = ApiUtility.ok("Hello");
        assertThat(okResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(okResponse.getBody().isSuccess()).isTrue();
        assertThat(okResponse.getBody().getData()).isEqualTo("Hello");

        ResponseEntity<ApiResponse<String>> createdResponse = ApiUtility.created("Item created", "NewItem");
        assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdResponse.getBody().getMessage()).isEqualTo("Item created");

        Page<String> page = new PageImpl<>(List.of("A", "B"), PageRequest.of(0, 10), 2);
        ResponseEntity<ApiResponse<PageResponse<String>>> pageResponse = ApiUtility.page(page);
        assertThat(pageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pageResponse.getBody().getData().getContent()).containsExactly("A", "B");
    }

    // --- RedisUtility Tests ---
    @Test
    @DisplayName("RedisUtility: builds structured keys and safely handles missing template")
    void testRedisUtility() {
        String key = RedisUtility.buildKey("user", UUID.randomUUID(), "profile");
        assertThat(key).startsWith("user:");
        assertThat(key).endsWith(":profile");

        RedisUtility redisUtility = new RedisUtility(null);
        assertThat(redisUtility.isAvailable()).isFalse();
        assertThat(redisUtility.get("some-key")).isEmpty();
        assertThat(redisUtility.hasKey("some-key")).isFalse();
        // Should not throw NPE when template is null
        redisUtility.set("key", "val", Duration.ofMinutes(1));
        redisUtility.delete("key");
    }

    // --- KafkaUtility Tests ---
    @Test
    @DisplayName("KafkaUtility: safely handles null template without throwing")
    void testKafkaUtility() {
        KafkaUtility kafkaUtility = new KafkaUtility(null);
        assertThat(kafkaUtility.isKafkaAvailable()).isFalse();
        // Should not throw NPE when Kafka is disabled/null
        kafkaUtility.publish("test-topic", "test-key", "test-payload");
        kafkaUtility.publish("test-key", "test-payload");
    }

    // --- EnumUtility Tests ---
    @Test
    @DisplayName("EnumUtility: parses case-insensitively, handles defaults, and validates correctly")
    void testEnumUtility() {
        // Valid case-insensitive parsing
        HttpStatus status = EnumUtility.fromString(HttpStatus.class, "ok");
        assertThat(status).isEqualTo(HttpStatus.OK);

        status = EnumUtility.fromString(HttpStatus.class, "CREATED");
        assertThat(status).isEqualTo(HttpStatus.CREATED);

        // Fallback with default
        HttpStatus notFound = EnumUtility.fromStringOrDefault(HttpStatus.class, "non_existent", HttpStatus.NOT_FOUND);
        assertThat(notFound).isEqualTo(HttpStatus.NOT_FOUND);

        HttpStatus okDefault = EnumUtility.fromStringOrDefault(HttpStatus.class, "ok", HttpStatus.BAD_REQUEST);
        assertThat(okDefault).isEqualTo(HttpStatus.OK);

        // Optional
        assertThat(EnumUtility.fromStringOptional(HttpStatus.class, "ok")).contains(HttpStatus.OK);
        assertThat(EnumUtility.fromStringOptional(HttpStatus.class, "invalid")).isEmpty();
        assertThat(EnumUtility.fromStringOptional(HttpStatus.class, null)).isEmpty();

        // Validation
        assertThat(EnumUtility.isValid(HttpStatus.class, "ok")).isTrue();
        assertThat(EnumUtility.isValid(HttpStatus.class, "BAD_REQUEST")).isTrue();
        assertThat(EnumUtility.isValid(HttpStatus.class, "unknown_value")).isFalse();
        assertThat(EnumUtility.isValid(HttpStatus.class, null)).isFalse();

        // Exception on invalid fromString
        assertThatThrownBy(() -> EnumUtility.fromString(HttpStatus.class, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid HttpStatus value");
        assertThatThrownBy(() -> EnumUtility.fromString(HttpStatus.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- ApiResponseBuilder Tests ---
    @Test
    @DisplayName("ApiResponseBuilder: builds success and failure responses")
    void testApiResponseBuilder() {
        ApiResponse<String> success = ApiResponseBuilder.success("Operation succeeded", "payload");
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getMessage()).isEqualTo("Operation succeeded");
        assertThat(success.getData()).isEqualTo("payload");

        ApiResponse<String> failure = ApiResponseBuilder.failure("Validation failed", "errorData");
        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getMessage()).isEqualTo("Validation failed");
        assertThat(failure.getData()).isEqualTo("errorData");

        ApiResponse<Void> simpleFailure = ApiResponseBuilder.failure("Something went wrong");
        assertThat(simpleFailure.isSuccess()).isFalse();
        assertThat(simpleFailure.getMessage()).isEqualTo("Something went wrong");
    }

    // --- PaginationResponseBuilder Tests ---
    @Test
    @DisplayName("PaginationResponseBuilder: correctly maps Page to PageResponse with sort metadata")
    void testPaginationResponseBuilder() {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
        );
        Page<String> page = new PageImpl<>(List.of("post1", "post2"), PageRequest.of(0, 10, sort), 2);

        PageResponse<String> response = PaginationResponseBuilder.build(page);
        assertThat(response.getContent()).containsExactly("post1", "post2");
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
        assertThat(response.isFirstPage()).isTrue();
        assertThat(response.isLastPage()).isTrue();
        assertThat(response.getEmpty()).isFalse();
        assertThat(response.getSorting()).hasSize(1);
        assertThat(response.getSorting().get(0).getField()).isEqualTo("createdAt");
        assertThat(response.getSorting().get(0).getDirection()).isEqualTo("DESC");
    }
}
