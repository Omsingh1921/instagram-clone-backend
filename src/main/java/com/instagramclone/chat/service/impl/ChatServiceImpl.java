package com.instagramclone.chat.service.impl;

import com.instagramclone.chat.constants.ChatConstants;
import com.instagramclone.chat.dto.request.AddGroupMembersRequest;
import com.instagramclone.chat.dto.request.AddMessageReactionRequest;
import com.instagramclone.chat.dto.request.CreateGroupConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.dto.request.UpdateGroupConversationRequest;
import com.instagramclone.chat.dto.response.ConversationResponse;
import com.instagramclone.chat.dto.response.MessageReactionResponse;
import com.instagramclone.chat.dto.response.MessageResponse;
import com.instagramclone.chat.entity.Conversation;
import com.instagramclone.chat.entity.ConversationMember;
import com.instagramclone.chat.entity.Message;
import com.instagramclone.chat.entity.MessageReaction;
import com.instagramclone.chat.enums.ConversationRole;
import com.instagramclone.chat.enums.ConversationType;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.chat.repository.ConversationMemberRepository;
import com.instagramclone.chat.repository.ConversationRepository;
import com.instagramclone.chat.repository.MessageReactionRepository;
import com.instagramclone.chat.repository.MessageRepository;
import com.instagramclone.chat.service.interfaces.IChatService;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID recipientId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException(ChatConstants.ERR_CANNOT_CHAT_SELF);
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(recipientId, currentUserId)) {
            throw new BadRequestException(ChatConstants.ERR_CANNOT_CHAT_USER);
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, recipientId)) {
            throw new BadRequestException(ChatConstants.ERR_BLOCKED_USER);
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_RECIPIENT_NOT_FOUND + recipientId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CURRENT_USER_NOT_FOUND));

        Optional<Conversation> existing = conversationRepository.findDirectConversationBetweenUsers(
                ConversationType.DIRECT, currentUserId, recipientId
        );

        if (existing.isPresent()) {
            return toConversationResponse(existing.get(), currentUserId);
        }

        // Create new direct conversation
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .build();
        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMember m1 = ConversationMember.builder()
                .conversation(savedConversation)
                .user(currentUser)
                .role(ConversationRole.MEMBER)
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .conversation(savedConversation)
                .user(recipient)
                .role(ConversationRole.MEMBER)
                .build();

        conversationMemberRepository.save(m1);
        conversationMemberRepository.save(m2);

        List<ConversationMember> memberList = new ArrayList<>();
        memberList.add(m1);
        memberList.add(m2);
        savedConversation.setMembers(memberList);
        log.info("Created new direct conversation {} between {} and {}", savedConversation.getId(), currentUserId, recipientId);

        return toConversationResponse(savedConversation, currentUserId);
    }

    @Override
    @Transactional
    public ConversationResponse createGroupConversation(CreateGroupConversationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CURRENT_USER_NOT_FOUND));

        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            throw new BadRequestException("At least one member is required to create a group");
        }

        List<UUID> distinctMemberIds = request.getMemberIds().stream()
                .filter(id -> id != null && !id.equals(currentUserId))
                .distinct()
                .toList();

        if (distinctMemberIds.isEmpty()) {
            throw new BadRequestException("Cannot create a group conversation with only yourself");
        }

        for (UUID memberId : distinctMemberIds) {
            if (userBlockRepository.existsByBlockerIdAndBlockedId(memberId, currentUserId) ||
                    userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, memberId)) {
                throw new BadRequestException("Cannot add a blocked user to a group conversation");
            }
        }

        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(request.getName().trim())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null)
                .creator(currentUser)
                .build();
        Conversation savedConversation = conversationRepository.save(conversation);

        List<ConversationMember> members = new ArrayList<>();

        ConversationMember adminMember = ConversationMember.builder()
                .conversation(savedConversation)
                .user(currentUser)
                .role(ConversationRole.ADMIN)
                .build();
        members.add(conversationMemberRepository.save(adminMember));

        for (UUID memberId : distinctMemberIds) {
            User memberUser = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member user not found with ID: " + memberId));
            ConversationMember member = ConversationMember.builder()
                    .conversation(savedConversation)
                    .user(memberUser)
                    .role(ConversationRole.MEMBER)
                    .build();
            members.add(conversationMemberRepository.save(member));
        }

        savedConversation.setMembers(members);
        log.info("Created group conversation {} '{}' with {} members", savedConversation.getId(), savedConversation.getName(), members.size());

        return toConversationResponse(savedConversation, currentUserId);
    }

    @Override
    @Transactional
    public ConversationResponse updateGroupConversation(UUID conversationId, UpdateGroupConversationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CONVERSATION_NOT_FOUND + conversationId));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException(ChatConstants.ERR_NOT_GROUP);
        }

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT));

        if (member.getRole() != ConversationRole.ADMIN) {
            throw new ForbiddenException(ChatConstants.ERR_ONLY_GROUP_ADMIN);
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            conversation.setName(request.getName().trim());
        }
        if (request.getAvatarUrl() != null) {
            conversation.setAvatarUrl(request.getAvatarUrl().trim());
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        Conversation updated = conversationRepository.save(conversation);

        ConversationResponse response = toConversationResponse(updated, currentUserId);
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "GROUP_UPDATED",
                    "conversationId", conversationId,
                    "name", updated.getName()
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast group update", ex);
        }

        return response;
    }

    @Override
    @Transactional
    public ConversationResponse addGroupMembers(UUID conversationId, AddGroupMembersRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CONVERSATION_NOT_FOUND + conversationId));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException(ChatConstants.ERR_NOT_GROUP);
        }

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        List<UUID> toAdd = request.getMemberIds().stream()
                .filter(id -> id != null && !conversationMemberRepository.existsByConversationIdAndUserId(conversationId, id))
                .distinct()
                .toList();

        for (UUID newMemberId : toAdd) {
            if (userBlockRepository.existsByBlockerIdAndBlockedId(newMemberId, currentUserId) ||
                    userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, newMemberId)) {
                continue;
            }
            User newMemberUser = userRepository.findById(newMemberId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + newMemberId));

            ConversationMember cm = ConversationMember.builder()
                    .conversation(conversation)
                    .user(newMemberUser)
                    .role(ConversationRole.MEMBER)
                    .build();
            conversationMemberRepository.save(cm);
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        ConversationResponse response = toConversationResponse(conversation, currentUserId);
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "MEMBERS_ADDED",
                    "conversationId", conversationId,
                    "addedCount", toAdd.size()
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast members added", ex);
        }

        return response;
    }

    @Override
    @Transactional
    public void removeGroupMember(UUID conversationId, UUID memberId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CONVERSATION_NOT_FOUND + conversationId));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException(ChatConstants.ERR_NOT_GROUP);
        }

        ConversationMember requester = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT));

        if (requester.getRole() != ConversationRole.ADMIN && !currentUserId.equals(memberId)) {
            throw new ForbiddenException(ChatConstants.ERR_ONLY_GROUP_ADMIN);
        }

        ConversationMember targetMember = conversationMemberRepository.findByConversationIdAndUserId(conversationId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this group"));

        conversationMemberRepository.delete(targetMember);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "MEMBER_REMOVED",
                    "conversationId", conversationId,
                    "memberId", memberId
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast member removed", ex);
        }
    }

    @Override
    @Transactional
    public void leaveGroup(UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        removeGroupMember(conversationId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getConversations(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<Conversation> page = conversationRepository.findConversationsByUserId(currentUserId, pageable);
        return PageResponse.from(page.map(c -> toConversationResponse(c, currentUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CONVERSATION_NOT_FOUND + conversationId));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        return toConversationResponse(conversation, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessages(UUID conversationId, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return PageResponse.from(messages.map(this::toMessageResponse));
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CONVERSATION_NOT_FOUND + conversationId));

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_UNAUTHORIZED_SEND);
        }

        if (conversation.getType() == ConversationType.DIRECT) {
            List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversationId);
            for (ConversationMember member : members) {
                UUID memberId = member.getUser().getId();
                if (!memberId.equals(currentUserId)) {
                    if (userBlockRepository.existsByBlockerIdAndBlockedId(memberId, currentUserId) ||
                            userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, memberId)) {
                        throw new ForbiddenException(ChatConstants.ERR_BLOCKED_CONVERSATION);
                    }
                }
            }
        }

        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasMedia = request.getMediaUrl() != null && !request.getMediaUrl().trim().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new BadRequestException(ChatConstants.ERR_EMPTY_MESSAGE);
        }

        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_SENDER_NOT_FOUND));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(hasContent ? request.getContent().trim() : null)
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .mediaUrl(hasMedia ? request.getMediaUrl().trim() : null)
                .build();

        Message savedMessage = messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(savedMessage);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
            log.info("Broadcasted message {} to /topic/conversations/{}", savedMessage.getId(), conversationId);
        } catch (Exception ex) {
            log.error("Failed to broadcast WebSocket message", ex);
        }

        return response;
    }

    @Override
    @Transactional
    public void unsendMessage(UUID conversationId, UUID messageId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_MESSAGE_NOT_FOUND + messageId));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        if (!message.getSender().getId().equals(currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_MESSAGE_SENDER);
        }

        message.softDelete();
        messageReactionRepository.deleteByMessageId(messageId);
        messageRepository.save(message);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "MESSAGE_DELETED",
                    "conversationId", conversationId,
                    "messageId", messageId
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast message unsend", ex);
        }
    }

    @Override
    @Transactional
    public MessageReactionResponse reactToMessage(UUID conversationId, UUID messageId, AddMessageReactionRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_MESSAGE_NOT_FOUND + messageId));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ChatConstants.ERR_CURRENT_USER_NOT_FOUND));

        Optional<MessageReaction> existingReaction = messageReactionRepository.findByMessageIdAndUserId(messageId, currentUserId);
        MessageReaction saved;
        if (existingReaction.isPresent()) {
            MessageReaction r = existingReaction.get();
            r.setEmoji(request.getEmoji().trim());
            saved = messageReactionRepository.save(r);
        } else {
            MessageReaction r = MessageReaction.builder()
                    .message(message)
                    .user(currentUser)
                    .emoji(request.getEmoji().trim())
                    .build();
            saved = messageReactionRepository.save(r);
        }

        MessageReactionResponse response = MessageReactionResponse.builder()
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .displayName(currentUser.getProfile() != null ? currentUser.getProfile().getDisplayName() : null)
                .emoji(saved.getEmoji())
                .createdAt(saved.getCreatedAt())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "MESSAGE_REACTION",
                    "conversationId", conversationId,
                    "messageId", messageId,
                    "userId", currentUserId,
                    "emoji", saved.getEmoji()
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast reaction", ex);
        }

        return response;
    }

    @Override
    @Transactional
    public void removeReaction(UUID conversationId, UUID messageId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ForbiddenException(ChatConstants.ERR_NOT_PARTICIPANT);
        }

        messageReactionRepository.deleteByMessageIdAndUserId(messageId, currentUserId);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "MESSAGE_REACTION_REMOVED",
                    "conversationId", conversationId,
                    "messageId", messageId,
                    "userId", currentUserId
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast reaction removed", ex);
        }
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ForbiddenException(ChatConstants.ERR_NOT_MEMBER));

        LocalDateTime readTime = LocalDateTime.now();
        member.setLastReadAt(readTime);
        conversationMemberRepository.save(member);
        log.info("User {} marked conversation {} as read", currentUserId, conversationId);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, Map.of(
                    "eventType", "READ_RECEIPT",
                    "conversationId", conversationId,
                    "userId", currentUserId,
                    "readAt", readTime.toString()
            ));
        } catch (Exception ex) {
            log.error("Failed to broadcast read receipt", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateTypingPermission(UUID conversationId, UUID userId) {
        if (conversationId == null || userId == null) {
            throw new BadRequestException(ChatConstants.ERR_CONVERSATION_AND_USER_ID_NULL);
        }

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException(ChatConstants.ERR_UNAUTHORIZED_TYPING);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ForbiddenException(ChatConstants.ERR_UNAUTHORIZED_TYPING));

        if (conversation.getType() == ConversationType.DIRECT) {
            List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversationId);
            for (ConversationMember member : members) {
                UUID memberId = member.getUser().getId();
                if (!memberId.equals(userId)) {
                    if (userBlockRepository.existsByBlockerIdAndBlockedId(memberId, userId) ||
                            userBlockRepository.existsByBlockerIdAndBlockedId(userId, memberId)) {
                        throw new ForbiddenException(ChatConstants.ERR_BLOCKED_TYPING);
                    }
                }
            }
        }
    }

    private ConversationResponse toConversationResponse(Conversation c, UUID currentUserId) {
        List<UserSummaryResponse> members = new ArrayList<>();
        LocalDateTime lastReadAt = null;

        List<ConversationMember> memberList = conversationMemberRepository.findByConversationId(c.getId());
        for (ConversationMember cm : memberList) {
            User u = cm.getUser();
            members.add(UserSummaryResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .build());

            if (u.getId().equals(currentUserId)) {
                lastReadAt = cm.getLastReadAt();
            }
        }

        MessageResponse lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(c.getId())
                .map(this::toMessageResponse)
                .orElse(null);

        long unreadCount = 0;
        if (lastReadAt != null) {
            unreadCount = messageRepository.countByConversationIdAndCreatedAtAfter(c.getId(), lastReadAt);
        } else if (lastMessage != null) {
            unreadCount = messageRepository.countByConversationIdAndCreatedAtAfter(c.getId(), c.getCreatedAt());
        }

        return ConversationResponse.builder()
                .id(c.getId())
                .type(c.getType())
                .name(c.getName())
                .avatarUrl(c.getAvatarUrl())
                .creatorId(c.getCreator() != null ? c.getCreator().getId() : null)
                .members(members)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message m) {
        User s = m.getSender();
        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(m.getId());
        List<MessageReactionResponse> reactionResponses = reactions.stream()
                .map(r -> MessageReactionResponse.builder()
                        .userId(r.getUser().getId())
                        .username(r.getUser().getUsername())
                        .displayName(r.getUser().getProfile() != null ? r.getUser().getProfile().getDisplayName() : null)
                        .emoji(r.getEmoji())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        boolean isDeleted = Boolean.TRUE.equals(m.getIsDeleted());

        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .sender(PostAuthorResponse.builder()
                        .id(s.getId())
                        .username(s.getUsername())
                        .displayName(s.getProfile() != null ? s.getProfile().getDisplayName() : null)
                        .avatarUrl(s.getProfile() != null ? s.getProfile().getAvatarUrl() : null)
                        .build())
                .content(isDeleted ? "This message was unsent" : m.getContent())
                .messageType(m.getMessageType())
                .mediaUrl(isDeleted ? null : m.getMediaUrl())
                .isDeleted(isDeleted)
                .reactions(reactionResponses)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
