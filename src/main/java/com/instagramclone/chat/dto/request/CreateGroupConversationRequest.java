package com.instagramclone.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupConversationRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;

    @NotEmpty(message = "Group must have at least one additional member")
    @Size(max = 50, message = "Group cannot exceed 50 members")
    private List<UUID> memberIds;

    @Size(max = 700, message = "Avatar URL must not exceed 700 characters")
    private String avatarUrl;
}
