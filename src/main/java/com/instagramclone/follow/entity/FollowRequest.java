package com.instagramclone.follow.entity;

import com.instagramclone.common.entity.BaseAuditableEntity;
import lombok.experimental.SuperBuilder;
import com.instagramclone.follow.enums.FollowRequestStatus;

import com.instagramclone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follow_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_follow_req_pair", columnNames = {"requester_id", "target_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class FollowRequest extends BaseAuditableEntity {
@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, columnDefinition = "BINARY(16)")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FollowRequestStatus status = FollowRequestStatus.PENDING;
}
