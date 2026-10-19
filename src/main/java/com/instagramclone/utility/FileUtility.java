package com.instagramclone.utility;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Reusable media/file validation adhering to Section 49.7 (File Utility).
 * Enforces magic-byte signature verification, content-type allowlisting,
 * file-size constraints, and decompression bomb mitigation.
 */
public final class FileUtility {

    private static final Logger log = LoggerFactory.getLogger(FileUtility.class);

    public static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    public static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/quicktime", "video/webm"
    );

    public static final long MAX_IMAGE_SIZE_BYTES = ValidationConstants.MAX_IMAGE_SIZE_BYTES;
    public static final long MAX_VIDEO_SIZE_BYTES = ValidationConstants.MAX_VIDEO_SIZE_BYTES;
    public static final long MAX_IMAGE_PIXELS = ValidationConstants.MAX_IMAGE_PIXELS;

    private FileUtility() {}

    /**
     * Validates an uploaded multipart file against MIME allowlists, magic-byte signatures,
     * size limits, and pixel decompression bounds.
     */
    public static void validateFile(MultipartFile file, boolean allowVideo) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("File content type cannot be determined");
        }

        String normalizedContentType = contentType.toLowerCase().trim();
        boolean isImage = ALLOWED_IMAGE_TYPES.contains(normalizedContentType);
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(normalizedContentType);

        if (!isImage && (!allowVideo || !isVideo)) {
            throw new BadRequestException("Unsupported file format: " + contentType);
        }

        long maxSize = isVideo ? MAX_VIDEO_SIZE_BYTES : MAX_IMAGE_SIZE_BYTES;
        if (file.getSize() > maxSize) {
            throw new BadRequestException("File size exceeds maximum permitted limit of " + (maxSize / (1024 * 1024)) + "MB");
        }

        // Validate magic byte signatures to prevent MIME-type spoofing
        validateMagicBytes(file, isImage, isVideo);

        // Mitigate image decompression bomb attacks
        if (isImage) {
            validateImageDimensions(file);
        }
    }

    /**
     * Inspects binary header magic bytes to verify actual media payload format.
     */
    public static void validateMagicBytes(MultipartFile file, boolean isImage, boolean isVideo) {
        byte[] header = new byte[16];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.read(header);
        } catch (IOException ex) {
            log.error("Failed to read file header bytes: {}", ex.getMessage());
            throw new BadRequestException("Could not read file binary header");
        }

        if (bytesRead < 4) {
            throw new BadRequestException("File header is truncated or malformed");
        }

        boolean matched = false;
        if (isImage) {
            matched = isJpegHeader(header, bytesRead)
                    || isPngHeader(header, bytesRead)
                    || isGifHeader(header, bytesRead)
                    || isWebpHeader(header, bytesRead);
        }
        if (!matched && isVideo) {
            matched = isMp4OrMovHeader(header, bytesRead)
                    || isWebmHeader(header, bytesRead);
        }

        if (!matched) {
            throw new BadRequestException("File signature does not match declared media type");
        }
    }

    private static boolean isJpegHeader(byte[] h, int len) {
        return len >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPngHeader(byte[] h, int len) {
        return len >= 8
                && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
    }

    private static boolean isGifHeader(byte[] h, int len) {
        return len >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
    }

    private static boolean isWebpHeader(byte[] h, int len) {
        if (len < 12) return false;
        return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }

    private static boolean isMp4OrMovHeader(byte[] h, int len) {
        if (len < 8) return false;
        // ISO base media file format / QuickTime ftyp or box signature at offset 4
        String marker = new String(h, 4, 4);
        return "ftyp".equals(marker) || "moov".equals(marker) || "mdat".equals(marker) || "wide".equals(marker);
    }

    private static boolean isWebmHeader(byte[] h, int len) {
        return len >= 4
                && (h[0] & 0xFF) == 0x1A && (h[1] & 0xFF) == 0x45
                && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3;
    }

    /**
     * Inspects image dimensions using lightweight header readers to mitigate decompression bombs.
     */
    private static void validateImageDimensions(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            if (iis == null) {
                return; // Custom format or reader not available in JDK
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    long totalPixels = (long) width * height;
                    if (totalPixels > MAX_IMAGE_PIXELS) {
                        throw new BadRequestException("Image dimensions " + width + "x" + height
                                + " exceed maximum permitted limit of " + (MAX_IMAGE_PIXELS / 1_000_000) + " megapixels");
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("Skipping dimensional validation: {}", ex.getMessage());
        }
    }

    public static String generateUniqueFilename(String originalFilename) {
        String ext = extractExtension(originalFilename);
        return UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    }

    public static String extractExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file_" + System.currentTimeMillis();
        }
        // Prevent path traversal and control characters
        String clean = filename.replace("\\", "/");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        return clean.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }
}
