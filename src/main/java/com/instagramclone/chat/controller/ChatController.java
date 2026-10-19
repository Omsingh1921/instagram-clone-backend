package com.instagramclone.chat.controller;

import com.instagramclone.chat.constants.ChatConstants;
import com.instagramclone.chat.dto.request.AddGroupMembersRequest;
import com.instagramclone.chat.dto.request.AddMessageReactionRequest;
import com.instagramclone.chat.dto.request.CreateConversationRequest;
import com.instagramclone.chat.dto.request.CreateGroupConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.dto.request.UpdateGroupConversationRequest;
import com.instagramclone.chat.dto.response.ConversationResponse;
import com.instagramclone.chat.dto.response.MessageReactionResponse;
import com.instagramclone.chat.dto.response.MessageResponse;
import com.instagramclone.chat.service.interfaces.IChatService;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.ratelimit.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ChatConstants.BASE_PATH)
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = chatService.getOrCreateDirectConversation(request.getRecipientId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ChatConstants.MSG_CONVERSATION_READY, response));
    }

    @PostMapping(ChatConstants.CONVERSATION_GROUP_PATH)
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request) {
        ConversationResponse response = chatService.createGroupConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ChatConstants.MSG_GROUP_CREATED, response));
    }

    @PutMapping(ChatConstants.CONVERSATION_GROUP_DETAILS_PATH)
    public ResponseEntity<ApiResponse<ConversationResponse>> updateGroupConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateGroupConversationRequest request) {
        ConversationResponse response = chatService.updateGroupConversation(conversationId, request);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_GROUP_UPDATED, response));
    }

    @PostMapping(ChatConstants.CONVERSATION_MEMBERS_PATH)
    public ResponseEntity<ApiResponse<ConversationResponse>> addGroupMembers(
            @PathVariable UUID conversationId,
            @Valid @RequestBody AddGroupMembersRequest request) {
        ConversationResponse response = chatService.addGroupMembers(conversationId, request);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_MEMBERS_ADDED, response));
    }

    @DeleteMapping(ChatConstants.CONVERSATION_MEMBER_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> removeGroupMember(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberId) {
        chatService.removeGroupMember(conversationId, memberId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_MEMBER_REMOVED, null));
    }

    @PostMapping(ChatConstants.CONVERSATION_LEAVE_PATH)
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @PathVariable UUID conversationId) {
        chatService.leaveGroup(conversationId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_LEFT_GROUP, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getConversations(
            @PageableDefault(size = ChatConstants.DEFAULT_CONVERSATIONS_PAGE_SIZE, sort = ChatConstants.SORT_BY_UPDATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ConversationResponse> response = chatService.getConversations(pageable);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_CONVERSATIONS_RETRIEVED, response));
    }

    @GetMapping(ChatConstants.CONVERSATION_BY_ID_PATH)
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(
            @PathVariable UUID conversationId) {
        ConversationResponse response = chatService.getConversationById(conversationId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_CONVERSATION_RETRIEVED, response));
    }

    @GetMapping(ChatConstants.CONVERSATION_MESSAGES_PATH)
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable UUID conversationId,
            @PageableDefault(size = ChatConstants.DEFAULT_MESSAGES_PAGE_SIZE, sort = ChatConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MessageResponse> response = chatService.getMessages(conversationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_MESSAGES_RETRIEVED, response));
    }

    @RateLimit(category = ChatConstants.RATE_LIMIT_MESSAGING, limit = 60, windowSeconds = 60)
    @PostMapping(ChatConstants.CONVERSATION_MESSAGES_PATH)
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.sendMessage(conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ChatConstants.MSG_MESSAGE_SENT, response));
    }

    @DeleteMapping(ChatConstants.CONVERSATION_MESSAGE_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> unsendMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        chatService.unsendMessage(conversationId, messageId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_MESSAGE_UNSENT, null));
    }

    @PostMapping(ChatConstants.CONVERSATION_MESSAGE_REACTIONS_PATH)
    public ResponseEntity<ApiResponse<MessageReactionResponse>> reactToMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @Valid @RequestBody AddMessageReactionRequest request) {
        MessageReactionResponse response = chatService.reactToMessage(conversationId, messageId, request);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_REACTION_ADDED, response));
    }

    @DeleteMapping(ChatConstants.CONVERSATION_MESSAGE_REACTIONS_PATH)
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        chatService.removeReaction(conversationId, messageId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_REACTION_REMOVED, null));
    }

    @PostMapping(ChatConstants.CONVERSATION_READ_PATH)
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @PathVariable UUID conversationId) {
        chatService.markConversationAsRead(conversationId);
        return ResponseEntity.ok(ApiResponse.success(ChatConstants.MSG_CONVERSATION_MARKED_READ, null));
    }
}
