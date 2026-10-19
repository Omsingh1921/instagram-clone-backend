package com.instagramclone.chat.dto.request;

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
public class AddGroupMembersRequest {

    @NotEmpty(message = "Member IDs list must not be empty")
    @Size(max = 50, message = "Cannot add more than 50 members at once")
    private List<UUID> memberIds;
}
