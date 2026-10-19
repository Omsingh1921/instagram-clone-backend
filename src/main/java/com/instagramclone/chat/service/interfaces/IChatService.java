package com.instagramclone.chat.service.interfaces;


import java.util.UUID;
import com.instagramclone.chat.dto.request.AddGroupMembersRequest;
import com.instagramclone.chat.dto.request.AddMessageReactionRequest;
import com.instagramclone.chat.dto.request.CreateGroupConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.dto.request.UpdateGroupConversationRequest;
import com.instagramclone.chat.dto.response.ConversationResponse;
import com.instagramclone.chat.dto.response.MessageReactionResponse;
import com.instagramclone.chat.dto.response.MessageResponse;
import com.instagramclone.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface IChatService {

    ConversationResponse getOrCreateDirectConversation(UUID recipientId);

    ConversationResponse createGroupConversation(CreateGroupConversationRequest request);

    ConversationResponse updateGroupConversation(UUID conversationId, UpdateGroupConversationRequest request);

    ConversationResponse addGroupMembers(UUID conversationId, AddGroupMembersRequest request);

    void removeGroupMember(UUID conversationId, UUID memberId);

    void leaveGroup(UUID conversationId);

    PageResponse<ConversationResponse> getConversations(Pageable pageable);

    ConversationResponse getConversationById(UUID conversationId);

    PageResponse<MessageResponse> getMessages(UUID conversationId, Pageable pageable);

    MessageResponse sendMessage(UUID conversationId, SendMessageRequest request);

    void unsendMessage(UUID conversationId, UUID messageId);

    MessageReactionResponse reactToMessage(UUID conversationId, UUID messageId, AddMessageReactionRequest request);

    void removeReaction(UUID conversationId, UUID messageId);

    void markConversationAsRead(UUID conversationId);

    void validateTypingPermission(UUID conversationId, UUID userId);
}
