package com.instagramclone.media;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.media.entity.Media;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CloudinaryMediaIntegrationTest extends BaseIntegrationTest {

    private User testUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        testUser = userRepository.save(User.builder()
                .username("media_tester")
                .email("media@instagram.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        userToken = jwtTokenProvider.generateTokenForUser(testUser.getId(), testUser.getUsername(), testUser.getEmail());
    }

    private static final byte[] VALID_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01
    };

    private static final byte[] VALID_PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
    };

    private static final byte[] VALID_MP4 = new byte[]{
            0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
            'm', 'p', '4', '2', 0x00, 0x00, 0x00, 0x00
    };

    @Test
    @DisplayName("Upload valid image: persists metadata in MySQL and returns MediaResponse")
    void testUploadValidImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                VALID_JPEG
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(imageFile)
                        .param("category", "posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").isNotEmpty())
                .andExpect(jsonPath("$.data.secureUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.resourceType").value("image"))
                .andExpect(jsonPath("$.data.mediaType").value("IMAGE"));

        List<Media> mediaList = mediaRepository.findByOwnerId(testUser.getId());
        assertThat(mediaList).hasSize(1);
        Media persisted = mediaList.get(0);
        assertThat(persisted.getPublicId()).contains("instagram-clone/posts/" + testUser.getId());
        assertThat(persisted.getOwner().getId()).isEqualTo(testUser.getId());
        assertThat(persisted.getResourceType()).isEqualTo("image");
    }

    @Test
    @DisplayName("Upload invalid MIME type: rejected with 400 Bad Request")
    void testUploadInvalidMimeType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                new byte[]{10, 20, 30}
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(invalidFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Upload valid video: persists video metadata correctly")
    void testUploadValidVideo() throws Exception {
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "clip.mp4",
                "video/mp4",
                VALID_MP4
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(videoFile)
                        .param("category", "reels")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceType").value("video"))
                .andExpect(jsonPath("$.data.mediaType").value("VIDEO"));
    }

    @Test
    @DisplayName("Delete media: authorized owner succeeds, unauthorized returns 403 Forbidden")
    void testDeleteMediaAuthorization() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .username("intruder")
                .email("intruder@instagram.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        String intruderToken = jwtTokenProvider.generateTokenForUser(otherUser.getId(), otherUser.getUsername(), otherUser.getEmail());

        // Upload media as testUser
        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "pic.png", "image/png", VALID_PNG
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(imageFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        Media media = mediaRepository.findByOwnerId(testUser.getId()).get(0);
        UUID mediaId = media.getId();

        // 1. Intruder attempts delete -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/media/" + mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        // 2. Owner deletes -> 200 OK
        mockMvc.perform(delete("/api/v1/media/" + mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(mediaRepository.findById(mediaId)).isEmpty();
    }

    @Test
    @DisplayName("Upload avatar: correctly partitions into users/{userId}/avatar folder")
    void testAvatarFolderPartitioning() throws Exception {
        MockMultipartFile avatarFile = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", VALID_JPEG
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(avatarFile)
                        .param("category", "avatar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<Media> mediaList = mediaRepository.findByOwnerId(testUser.getId());
        assertThat(mediaList).isNotEmpty();
        Media avatarMedia = mediaList.get(0);
        assertThat(avatarMedia.getFolder()).isEqualTo("instagram-clone/users/" + testUser.getId() + "/avatar");
    }

    @Test
    @DisplayName("Upload story: correctly partitions into stories/{userId} folder")
    void testStoriesFolderPartitioning() throws Exception {
        MockMultipartFile storyFile = new MockMultipartFile(
                "file", "story.jpg", "image/jpeg", VALID_JPEG
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(storyFile)
                        .param("category", "stories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<Media> mediaList = mediaRepository.findByOwnerId(testUser.getId());
        assertThat(mediaList).isNotEmpty();
        Media storyMedia = mediaList.get(0);
        assertThat(storyMedia.getFolder()).isEqualTo("instagram-clone/stories/" + testUser.getId());
    }

    @Test
    @DisplayName("Upload oversized file: rejected with 400 Bad Request")
    void testOversizedFileRejected() throws Exception {
        // Image limit is 10MB; upload 11MB file
        byte[] oversizedBytes = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png", oversizedBytes
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(largeFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("limit")));
    }
}
