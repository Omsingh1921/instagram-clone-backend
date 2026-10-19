package com.instagramclone.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryPollResponse {
    private UUID id;
    private String question;
    private String optionA;
    private String optionB;
    private long voteCountA;
    private long voteCountB;
    private long totalVotes;
    private double percentA;
    private double percentB;
    private Integer userVote;
}
