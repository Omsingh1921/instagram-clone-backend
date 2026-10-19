package com.instagramclone.utility;

import com.instagramclone.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilityEnhancedTest {

    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01
    };

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
    };

    private static final byte[] VALID_GIF_BYTES = new byte[]{
            'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00, 0x01, 0x00,
            (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static final byte[] VALID_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00,
            'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    private static final byte[] VALID_MP4_BYTES = new byte[]{
            0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
            'm', 'p', '4', '2', 0x00, 0x00, 0x00, 0x00
    };

    @Test
    @DisplayName("Valid JPEG file with matching magic bytes passes validation")
    void validJpeg_Passes() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);
        assertThatCode(() -> FileUtility.validateFile(file, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid PNG file with matching magic bytes passes validation")
    void validPng_Passes() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", VALID_PNG_BYTES);
        assertThatCode(() -> FileUtility.validateFile(file, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid GIF file with matching magic bytes passes validation")
    void validGif_Passes() {
        MockMultipartFile file = new MockMultipartFile("file", "anim.gif", "image/gif", VALID_GIF_BYTES);
        assertThatCode(() -> FileUtility.validateFile(file, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid WebP file with matching magic bytes passes validation")
    void validWebp_Passes() {
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", VALID_WEBP_BYTES);
        assertThatCode(() -> FileUtility.validateFile(file, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid MP4 video passes validation when allowVideo is true")
    void validMp4_WhenVideoAllowed_Passes() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", VALID_MP4_BYTES);
        assertThatCode(() -> FileUtility.validateFile(file, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Video file fails validation when allowVideo is false")
    void video_WhenVideoDisallowed_Fails() {
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", VALID_MP4_BYTES);
        assertThatThrownBy(() -> FileUtility.validateFile(file, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file format");
    }

    @Test
    @DisplayName("Spoofed MIME type with non-matching binary magic bytes fails validation")
    void spoofedMimeType_FailsMagicByteCheck() {
        byte[] phpScript = "<?php echo 'malicious payload'; ?>".getBytes();
        MockMultipartFile spoofedFile = new MockMultipartFile("file", "exploit.jpg", "image/jpeg", phpScript);

        assertThatThrownBy(() -> FileUtility.validateFile(spoofedFile, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File signature does not match declared media type");
    }

    @Test
    @DisplayName("Null and empty files throw BadRequestException")
    void emptyOrNullFile_ThrowsBadRequestException() {
        assertThatThrownBy(() -> FileUtility.validateFile(null, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Uploaded file cannot be empty");

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> FileUtility.validateFile(emptyFile, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Uploaded file cannot be empty");
    }

    @Test
    @DisplayName("Unsupported MIME type throws BadRequestException")
    void unsupportedMimeType_Throws() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        assertThatThrownBy(() -> FileUtility.validateFile(pdf, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file format");
    }

    @Test
    @DisplayName("Oversized image file throws BadRequestException")
    void oversizedImage_Throws() {
        // Create 11MB payload
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        System.arraycopy(VALID_JPEG_BYTES, 0, largeBytes, 0, VALID_JPEG_BYTES.length);
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> FileUtility.validateFile(largeFile, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File size exceeds maximum permitted limit");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "..\\..\\windows\\system32\\cmd.exe",
            "folder/subfolder/file.png"
    })
    @DisplayName("sanitizeFilename strips directory traversal sequences")
    void sanitizeFilename_StripsDirectoryTraversal(String path) {
        String sanitized = FileUtility.sanitizeFilename(path);
        assertThat(sanitized).doesNotContain("/");
        assertThat(sanitized).doesNotContain("\\");
        assertThat(sanitized).doesNotContain("..");
    }

    @Test
    @DisplayName("sanitizeFilename sanitizes illegal characters")
    void sanitizeFilename_ReplacesSpecialChars() {
        String clean = FileUtility.sanitizeFilename("my picture!@#$%^&*()_test.png");
        assertThat(clean).matches("^[a-zA-Z0-9._-]+$");
    }
}
