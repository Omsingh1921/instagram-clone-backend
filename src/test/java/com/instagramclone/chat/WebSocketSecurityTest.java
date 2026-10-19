package com.instagramclone.chat;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.entity.Conversation;
import com.instagramclone.chat.entity.ConversationMember;
import com.instagramclone.chat.enums.ConversationType;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.security.UserPrincipal;
import com.instagramclone.security.WebSocketAuthInterceptor;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.chat.controller.WebSocketChatController;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.UnauthorizedException;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.repository.UserBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WebSocketSecurityTest extends BaseIntegrationTest {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Autowired
    private ITokenBlacklistService tokenBlacklistService;

    @Autowired
    private WebSocketChatController webSocketChatController;

    @Autowired
    private IPresenceService presenceService;

    @Autowired
    private UserBlockRepository userBlockRepository;

    private User user1;
    private User user2;
    private User user3;
    private String user1Token;
    private Conversation conversation12;
    private MessageChannel dummyChannel;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        dummyChannel = mock(MessageChannel.class);

        user1 = userRepository.save(User.builder()
                .username("ws_user1")
                .email("ws1@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        user2 = userRepository.save(User.builder()
                .username("ws_user2")
                .email("ws2@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        user3 = userRepository.save(User.builder()
                .username("ws_user3")
                .email("ws3@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        user1Token = jwtTokenProvider.generateTokenForUser(user1.getId(), user1.getUsername(), user1.getEmail());

        conversation12 = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        conversationMemberRepository.save(ConversationMember.builder()
                .conversation(conversation12)
                .user(user1)
                .build());

        conversationMemberRepository.save(ConversationMember.builder()
                .conversation(conversation12)
                .user(user2)
                .build());
    }

    @Test
    @DisplayName("STOMP CONNECT - Should authenticate valid Bearer token")
    void connect_WithValidToken_ShouldSucceed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + user1Token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = webSocketAuthInterceptor.preSend(message, dummyChannel);
        assertThat(result).isNotNull();

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("ws_user1");
    }

    @Test
    @DisplayName("STOMP CONNECT - Should reject missing Authorization header")
    void connect_WithoutToken_ShouldThrowException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Missing or malformed Authorization header");
    }

    @Test
    @DisplayName("STOMP CONNECT - Should reject blacklisted token")
    void connect_WithBlacklistedToken_ShouldThrowException() {
        tokenBlacklistService.blacklistToken(user1Token, 60000L);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + user1Token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Token has been revoked");
    }

    @Test
    @DisplayName("STOMP CONNECT - Should reject unverified user token with Unauthorized")
    void connect_WithUnverifiedUserToken_ShouldThrowException() {
        User unverifiedUser = userRepository.save(User.builder()
                .username("ws_unverified")
                .email("ws_unverified@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        String unverifiedToken = jwtTokenProvider.generateTokenForUser(unverifiedUser.getId(), unverifiedUser.getUsername(), unverifiedUser.getEmail());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + unverifiedToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Email address is not verified");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Member should be allowed to subscribe to conversation topic")
    void subscribe_ToConversation_WhenMember_ShouldSucceed() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/conversations/" + conversation12.getId());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = webSocketAuthInterceptor.preSend(message, dummyChannel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Non-member should be FORBIDDEN from subscribing to conversation topic")
    void subscribe_ToConversation_WhenNotMember_ShouldBeForbidden() {
        // User3 is NOT a member of conversation12
        UserPrincipal principal = UserPrincipal.create(user3);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/conversations/" + conversation12.getId());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Forbidden: You are not a member of this conversation");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - User should be forbidden from subscribing to another user's notifications")
    void subscribe_ToOtherUserNotifications_ShouldBeForbidden() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/notifications/" + user2.getId());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Forbidden: Cannot subscribe to another user's notifications");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Member should be allowed to subscribe to typing topic")
    void subscribe_ToTypingTopic_WhenMember_ShouldSucceed() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/conversations/" + conversation12.getId() + "/typing");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = webSocketAuthInterceptor.preSend(message, dummyChannel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Non-member should be FORBIDDEN from subscribing to typing topic")
    void subscribe_ToTypingTopic_WhenNotMember_ShouldBeForbidden() {
        UserPrincipal principal = UserPrincipal.create(user3);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/conversations/" + conversation12.getId() + "/typing");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Forbidden: You are not a member of this conversation");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Should reject malformed conversation UUIDs with BadRequest")
    void subscribe_ToConversation_WhenMalformedUUID_ShouldBeBadRequest() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // Malformed UUID on main conversation topic
        StompHeaderAccessor accessorMain = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessorMain.setUser(auth);
        accessorMain.setDestination("/topic/conversations/not-a-valid-uuid");
        Message<byte[]> messageMain = MessageBuilder.createMessage(new byte[0], accessorMain.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(messageMain, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("BadRequest: Invalid conversation ID");

        // Malformed UUID on typing topic
        StompHeaderAccessor accessorTyping = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessorTyping.setUser(auth);
        accessorTyping.setDestination("/topic/conversations/invalid-uuid/typing");
        Message<byte[]> messageTyping = MessageBuilder.createMessage(new byte[0], accessorTyping.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(messageTyping, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("BadRequest: Invalid conversation ID");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Should reject invalid suffixes and malformed conversation paths with BadRequest")
    void subscribe_ToConversation_WhenInvalidSuffixOrPath_ShouldBeBadRequest() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String[] invalidDestinations = {
                "/topic/conversations",
                "/topic/conversations/",
                "/topic/conversations/" + conversation12.getId() + "/",
                "/topic/conversations/" + conversation12.getId() + "/typing/",
                "/topic/conversations/" + conversation12.getId() + "/messages",
                "/topic/conversations/" + conversation12.getId() + "/read",
                "/topic/conversations/" + conversation12.getId() + "/typing/extra",
                "/topic/conversations//typing"
        };

        for (String destination : invalidDestinations) {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setUser(auth);
            accessor.setDestination(destination);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("BadRequest: Invalid conversation destination");
        }
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Unauthenticated subscriptions must be rejected with Unauthorized")
    void subscribe_WhenUnauthenticated_ShouldBeUnauthorized() {
        String[] destinations = {
                "/topic/conversations/" + conversation12.getId(),
                "/topic/conversations/" + conversation12.getId() + "/typing",
                "/topic/notifications/" + user1.getId()
        };

        for (String destination : destinations) {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setUser(null);
            accessor.setDestination(destination);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, dummyChannel))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Unauthorized: Must be authenticated to subscribe");
        }

        // Invalid principal type (not UserPrincipal)
        Principal dummyPrincipal = () -> "anonymous";
        StompHeaderAccessor accessorDummy = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessorDummy.setUser(dummyPrincipal);
        accessorDummy.setDestination("/topic/conversations/" + conversation12.getId() + "/typing");
        Message<byte[]> messageDummy = MessageBuilder.createMessage(new byte[0], accessorDummy.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(messageDummy, dummyChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Unauthorized: Must be authenticated to subscribe");
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Owner should be allowed to subscribe to own notifications topic")
    void subscribe_ToNotifications_WhenOwner_ShouldSucceed() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/notifications/" + user1.getId());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = webSocketAuthInterceptor.preSend(message, dummyChannel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP TYPING - Authorized member should be allowed to set typing status")
    void handleTyping_WhenAuthorizedMember_ShouldSetTypingStatus() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isFalse();

        webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth);

        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isTrue();
    }

    @Test
    @DisplayName("STOMP TYPING - Authorized member should be allowed to clear typing status")
    void handleTyping_WhenAuthorizedMember_ShouldClearTypingStatus() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // Set typing
        webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth);
        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isTrue();

        // Clear typing
        webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", false), auth);
        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isFalse();
    }

    @Test
    @DisplayName("STOMP TYPING - Non-member should be FORBIDDEN from setting typing status")
    void handleTyping_WhenNonMember_ShouldThrowForbiddenAndNotSetTyping() {
        // User3 is NOT a member of conversation12
        UserPrincipal principal = UserPrincipal.create(user3);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to send typing indicators in this conversation");

        assertThat(presenceService.isTyping(conversation12.getId(), user3.getId())).isFalse();
    }

    @Test
    @DisplayName("STOMP TYPING - Non-member cannot clear or alter another conversation's typing status")
    void handleTyping_WhenNonMemberAttemptsToClearTyping_ShouldThrowForbiddenAndNotAlterState() {
        // User1 is actively typing in conversation12
        UserPrincipal principal1 = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth1 =
                new UsernamePasswordAuthenticationToken(principal1, null, principal1.getAuthorities());
        webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth1);
        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isTrue();

        // User3 (non-member) tries to clear typing state in conversation12
        UserPrincipal principal3 = UserPrincipal.create(user3);
        UsernamePasswordAuthenticationToken auth3 =
                new UsernamePasswordAuthenticationToken(principal3, null, principal3.getAuthorities());

        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", false), auth3))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to send typing indicators in this conversation");

        // User1's typing state remains unaffected
        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isTrue();
    }

    @Test
    @DisplayName("STOMP TYPING - Blocked users in direct conversation should be FORBIDDEN from typing")
    void handleTyping_WhenDirectConversationBlocked_ShouldThrowForbiddenAndNotSetTyping() {
        UserPrincipal principal1 = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth1 =
                new UsernamePasswordAuthenticationToken(principal1, null, principal1.getAuthorities());

        // Case A: User2 blocks User1
        userBlockRepository.save(UserBlock.builder()
                .blocker(user2)
                .blocked(user1)
                .build());

        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot send typing indicators in this conversation due to user block");

        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isFalse();

        // Case B: User1 blocks User2 (clean and recreate block from user1)
        userBlockRepository.deleteAll();
        userBlockRepository.save(UserBlock.builder()
                .blocker(user1)
                .blocked(user2)
                .build());

        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), auth1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot send typing indicators in this conversation due to user block");

        assertThat(presenceService.isTyping(conversation12.getId(), user1.getId())).isFalse();
    }

    @Test
    @DisplayName("STOMP TYPING - Missing or invalid authentication cannot be used to spoof typing status")
    void handleTyping_WhenUnauthenticatedOrInvalidPrincipal_ShouldThrowUnauthorized() {
        // Null principal
        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User must be authenticated to send typing indicators");

        // Dummy principal that is not UserPrincipal
        Principal dummyPrincipal = () -> "anonymousUser";
        assertThatThrownBy(() -> webSocketChatController.handleTyping(conversation12.getId(), Map.of("typing", true), dummyPrincipal))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User must be authenticated to send typing indicators");
    }

    @Test
    @DisplayName("STOMP TYPING - Non-existent conversation should be rejected without disclosing existence")
    void handleTyping_WhenNonExistentConversation_ShouldThrowForbiddenWithoutDisclosure() {
        UserPrincipal principal = UserPrincipal.create(user1);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UUID randomConversationId = UUID.randomUUID();

        assertThatThrownBy(() -> webSocketChatController.handleTyping(randomConversationId, Map.of("typing", true), auth))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not authorized to send typing indicators in this conversation");

        assertThat(presenceService.isTyping(randomConversationId, user1.getId())).isFalse();
    }
}
