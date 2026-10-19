package com.instagramclone.story.entity;

import com.instagramclone.common.entity.BaseCreatedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StoryPoll extends BaseCreatedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "question", nullable = false, length = 200)
    private String question;

    @Column(name = "option_a", nullable = false, length = 50)
    @Builder.Default
    private String optionA = "Yes";

    @Column(name = "option_b", nullable = false, length = 50)
    @Builder.Default
    private String optionB = "No";

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoryPollVote> votes = new ArrayList<>();
}
