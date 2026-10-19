package com.instagramclone.story.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Story extends BaseCreatedEntity {
@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "media_url", nullable = false, length = 700)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "is_close_friends_only", nullable = false)
    @Builder.Default
    private boolean isCloseFriendsOnly = false;

    @jakarta.persistence.OneToOne(mappedBy = "story", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private StoryPoll poll;

    @jakarta.persistence.OneToMany(mappedBy = "story", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<StoryView> views = new java.util.ArrayList<>();

    @jakarta.persistence.OneToMany(mappedBy = "story", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<StoryMention> mentions = new java.util.ArrayList<>();
}
