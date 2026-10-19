package com.instagramclone.post.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "post_media_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostMediaTag extends BaseCreatedEntity {
@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_media_id", nullable = false, columnDefinition = "BINARY(16)")
    private PostMedia postMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagged_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User taggedUser;

    @Column(name = "x_percent")
    private Double xPercent;

    @Column(name = "y_percent")
    private Double yPercent;
}
