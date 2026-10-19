package com.instagramclone.reel.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reel_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reel_likes", columnNames = {"user_id", "reel_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReelLike extends BaseCreatedEntity {
@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reel_id", nullable = false, columnDefinition = "BINARY(16)")
    private Reel reel;
}
