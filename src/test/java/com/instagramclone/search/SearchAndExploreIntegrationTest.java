package com.instagramclone.search;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.entity.PostMedia;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.enums.ReelStatus;
import com.instagramclone.search.dto.request.AddSearchHistoryRequest;
import com.instagramclone.search.enums.SearchType;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchAndExploreIntegrationTest extends BaseIntegrationTest {

    private User alice;
    private User bob;
    private User charlie;

    private String tokenAlice;
    private String tokenBob;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        alice = userRepository.save(User.builder()
                .username("alice_wonder")
                .email("alice@explore.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        profileRepository.save(Profile.builder()
                .user(alice)
                .displayName("Alice In Wonderland")
                .bio("Exploring the world")
                .build());

        bob = userRepository.save(User.builder()
                .username("bob_traveler")
                .email("bob@explore.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        profileRepository.save(Profile.builder()
                .user(bob)
                .displayName("Bob The Traveler")
                .bio("Travel and nature enthusiast")
                .build());

        charlie = userRepository.save(User.builder()
                .username("charlie_chef")
                .email("charlie@explore.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
        tokenBob = jwtTokenProvider.generateTokenForUser(bob.getId(), bob.getUsername(), bob.getEmail());
    }

    @Test
    @DisplayName("Explore Grid - Mix of Public Posts and Reels Ranked by Engagement")
    void exploreGrid_ShouldReturnPostsAndReels() throws Exception {
        // 1. Create a public post with media by Bob
        Post post = Post.builder()
                .user(bob)
                .caption("Sunset in mountains #travel #nature")
                .visibility(PostVisibility.PUBLIC)
                .build();
        Post savedPost = postRepository.save(post);

        PostMedia media = PostMedia.builder()
                .post(savedPost)
                .mediaUrl("https://res.cloudinary.com/demo/image/upload/mountain.jpg")
                .mediaType(MediaType.IMAGE)
                .displayOrder(0)
                .build();
        postMediaRepository.save(media);

        // 2. Create a ready reel by Charlie
        Reel reel = Reel.builder()
                .user(charlie)
                .videoUrl("https://res.cloudinary.com/demo/video/upload/cooking.mp4")
                .thumbnailUrl("https://res.cloudinary.com/demo/image/upload/cooking_thumb.jpg")
                .caption("Making pizza from scratch #foodie #cooking")
                .status(ReelStatus.READY)
                .durationMs(15000L)
                .build();
        reelRepository.save(reel);

        // 3. Alice requests explore grid
        mockMvc.perform(get("/api/v1/search/explore")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].type").exists())
                .andExpect(jsonPath("$.data.content[1].type").exists());
    }

    @Test
    @DisplayName("Search - User and Hashtag Search with Filtered Tag Posts")
    void searchUsersAndHashtags_ShouldReturnMatchingResults() throws Exception {
        // 1. Search Users by query "traveler"
        mockMvc.perform(get("/api/v1/search/users")
                        .param("q", "traveler")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("bob_traveler"));

        // 2. Create post with hashtag #wanderlust
        Post post = Post.builder()
                .user(bob)
                .caption("Wandering through ancient alleys #wanderlust")
                .visibility(PostVisibility.PUBLIC)
                .build();
        postRepository.save(post);

        // 3. Search Hashtags with query "wanderlust"
        mockMvc.perform(get("/api/v1/search/tags")
                        .param("q", "wanderlust")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("wanderlust"))
                .andExpect(jsonPath("$.data[0].postCount").value(1));

        // 4. Get posts for hashtag "wanderlust"
        mockMvc.perform(get("/api/v1/search/tags/{tag}/posts", "wanderlust")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].caption").value("Wandering through ancient alleys #wanderlust"));
    }

    @Test
    @DisplayName("Recent Search History - Record, Retrieve, and Clear")
    void recentSearchHistory_Lifecycle() throws Exception {
        // 1. Record search query
        AddSearchHistoryRequest req = AddSearchHistoryRequest.builder()
                .query("bob_traveler")
                .targetUserId(bob.getId())
                .searchType(SearchType.USER)
                .build();

        String recordRes = mockMvc.perform(post("/api/v1/search/recent")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.query").value("bob_traveler"))
                .andReturn().getResponse().getContentAsString();

        UUID historyId = UUID.fromString(objectMapper.readTree(recordRes).get("data").get("id").asText());

        // 2. Retrieve recent searches
        mockMvc.perform(get("/api/v1/search/recent")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].targetUser.username").value("bob_traveler"));

        // 3. Delete single search item
        mockMvc.perform(delete("/api/v1/search/recent/{id}", historyId)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 4. Verify history is empty
        mockMvc.perform(get("/api/v1/search/recent")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
