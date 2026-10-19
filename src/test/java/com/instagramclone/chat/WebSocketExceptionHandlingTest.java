package com.instagramclone.chat;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.controller.WebSocketChatController;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.entity.Conversation;
import com.instagramclone.chat.entity.ConversationMember;
import com.instagramclone.chat.enums.ConversationType;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.security.UserPrincipal;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.repository.UserBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WebSocketExceptionHandlingTest extends BaseIntegrationTest {

    @Autowired
    private WebSocketChatController webSocketChatController;

    @Autowired
    @Qualifier("clientInboundChannel")
    private SubscribableChannel clientInboundChannel;

    @Autowired
    @Qualifier("brokerChannel")
    private SubscribableChannel brokerChannel;

    @Autowired
    private UserBlockRepository userBlockRepository;

    private User user1;
    private User user2;
    private User user3;
    private Conversation conversation12;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user1 = userRepository.save(User.builder()
                .username("chat_user1")
                .email("user1@chat.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        user2 = userRepository.save(User.builder()
                .username("chat_user2")
                .email("user2@chat.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        user3 = userRepository.save(User.builder()
                .username("chat_user3")
                .email("user3@chat.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

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

    private Message<byte[]> createStompSendMessage(User sender, String destination, Object payload) throws Exception {
        UserPrincipal principal = UserPrincipal.create(sender);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-" + sender.getId());
        accessor.setUser(auth);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);

        byte[] payloadBytes = payload != null ? objectMapper.writeValueAsBytes(payload) : new byte[0];
        return MessageBuilder.createMessage(payloadBytes, accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("STOMP Errors - Expected application error in chat handler produces structured error response to user queue")
    void handleChatMessage_WhenEmptyContentAndMedia_ShouldRouteBadRequestToUserQueue() throws Exception {
        List<Message<?>> capturedMessages = new CopyOnWriteArrayList<>();
        CompletableFuture<Message<?>> errorFuture = new CompletableFuture<>();

        MessageHandler handler = m -> {
            capturedMessages.add(m);
            String dest = StompHeaderAccessor.wrap(m).getDestination();
            if (dest != null && dest.contains("/queue/errors")) {
                errorFuture.complete(m);
            }
        };
        brokerChannel.subscribe(handler);

        try {
            SendMessageRequest emptyRequest = SendMessageRequest.builder()
                    .content("   ")
                    .mediaUrl(null)
                    .build();

            Message<byte[]> message = createStompSendMessage(
                    user1,
                    "/app/chat.send/" + conversation12.getId(),
                    emptyRequest
            );

            clientInboundChannel.send(message);

            Message<?> errorMessage = errorFuture.get(5, TimeUnit.SECONDS);
            assertThat(errorMessage).as("Expected error message to be routed to /queue/errors").isNotNull();

            String payloadStr = new String((byte[]) errorMessage.getPayload(), StandardCharsets.UTF_8);
            ApiResponse<?> response = objectMapper.readValue(payloadStr, ApiResponse.class);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Message must contain either text content or media");
            assertThat(response.getErrors()).contains("BAD_REQUEST");
            assertThat(response.getPath()).isEqualTo("/app/chat.send/" + conversation12.getId());

            // Destination contains user identifier and queue
            String dest = StompHeaderAccessor.wrap(errorMessage).getDestination();
            assertThat(dest).contains("/queue/errors");
            assertThat(dest).contains(user1.getUsername());

            // Verify no message was broadcast to conversation topic
            boolean conversationBroadcast = capturedMessages.stream()
                    .anyMatch(m -> {
                        String d = StompHeaderAccessor.wrap(m).getDestination();
                        return d != null && d.equals("/topic/conversations/" + conversation12.getId());
                    });
            assertThat(conversationBroadcast).as("Errors must NOT be broadcast to conversation topic").isFalse();
        } finally {
            brokerChannel.unsubscribe(handler);
        }
    }

    @Test
    @DisplayName("STOMP Errors - Unauthorized conversation member produces safe Forbidden error response")
    void handleChatMessage_WhenNonMember_ShouldRouteForbiddenToUserQueue() throws Exception {
        List<Message<?>> capturedMessages = new CopyOnWriteArrayList<>();
        CompletableFuture<Message<?>> errorFuture = new CompletableFuture<>();

        MessageHandler handler = m -> {
            capturedMessages.add(m);
            String dest = StompHeaderAccessor.wrap(m).getDestination();
            if (dest != null && dest.contains("/queue/errors")) {
                errorFuture.complete(m);
            }
        };
        brokerChannel.subscribe(handler);

        try {
            // User3 is NOT a member of conversation12
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Unauthorized message from user3")
                    .build();

            Message<byte[]> message = createStompSendMessage(
                    user3,
                    "/app/chat.send/" + conversation12.getId(),
                    request
            );

            clientInboundChannel.send(message);

            Message<?> errorMessage = errorFuture.get(5, TimeUnit.SECONDS);
            assertThat(errorMessage).isNotNull();

            String payloadStr = new String((byte[]) errorMessage.getPayload(), StandardCharsets.UTF_8);
            ApiResponse<?> response = objectMapper.readValue(payloadStr, ApiResponse.class);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("You are not authorized to send messages in this conversation");
            assertThat(response.getErrors()).contains("FORBIDDEN");
            assertThat(response.getPath()).isEqualTo("/app/chat.send/" + conversation12.getId());

            // Ensure destination was targeted to user3
            String dest = StompHeaderAccessor.wrap(errorMessage).getDestination();
            assertThat(dest).contains(user3.getUsername());
        } finally {
            brokerChannel.unsubscribe(handler);
        }
    }

    @Test
    @DisplayName("STOMP Errors - Blocked conversation message produces safe Forbidden error response without leak")
    void handleChatMessage_WhenBlocked_ShouldRouteForbiddenError() throws Exception {
        // User2 blocks User1
        userBlockRepository.save(UserBlock.builder()
                .blocker(user2)
                .blocked(user1)
                .build());

        CompletableFuture<Message<?>> errorFuture = new CompletableFuture<>();
        MessageHandler handler = m -> {
            String dest = StompHeaderAccessor.wrap(m).getDestination();
            if (dest != null && dest.contains("/queue/errors")) {
                errorFuture.complete(m);
            }
        };
        brokerChannel.subscribe(handler);

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello while blocked")
                    .build();

            Message<byte[]> message = createStompSendMessage(
                    user1,
                    "/app/chat.send/" + conversation12.getId(),
                    request
            );

            clientInboundChannel.send(message);

            Message<?> errorMessage = errorFuture.get(5, TimeUnit.SECONDS);
            assertThat(errorMessage).isNotNull();

            String payloadStr = new String((byte[]) errorMessage.getPayload(), StandardCharsets.UTF_8);
            ApiResponse<?> response = objectMapper.readValue(payloadStr, ApiResponse.class);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Cannot send messages in this conversation due to user block");
            assertThat(response.getErrors()).contains("FORBIDDEN");
        } finally {
            brokerChannel.unsubscribe(handler);
        }
    }

    @Test
    @DisplayName("STOMP Errors - Null request body produces structured BadRequest error")
    void handleChatMessage_WhenNullPayload_ShouldRouteBadRequest() throws Exception {
        CompletableFuture<Message<?>> errorFuture = new CompletableFuture<>();
        MessageHandler handler = m -> {
            String dest = StompHeaderAccessor.wrap(m).getDestination();
            if (dest != null && dest.contains("/queue/errors")) {
                errorFuture.complete(m);
            }
        };
        brokerChannel.subscribe(handler);

        try {
            Message<byte[]> message = createStompSendMessage(
                    user1,
                    "/app/chat.send/" + conversation12.getId(),
                    null
            );

            clientInboundChannel.send(message);

            Message<?> errorMessage = errorFuture.get(5, TimeUnit.SECONDS);
            assertThat(errorMessage).isNotNull();

            String payloadStr = new String((byte[]) errorMessage.getPayload(), StandardCharsets.UTF_8);
            ApiResponse<?> response = objectMapper.readValue(payloadStr, ApiResponse.class);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Message request body must not be null");
            assertThat(response.getErrors()).contains("BAD_REQUEST");
        } finally {
            brokerChannel.unsubscribe(handler);
        }
    }

    @Test
    @DisplayName("STOMP Errors - Unexpected internal exception does NOT leak internal details or stack traces")
    void handleGeneralException_ShouldSanitizeErrorMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/" + conversation12.getId());
        Message<byte[]> dummyMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        RuntimeException sensitiveException = new RuntimeException(
                "Fatal error in org.hibernate.engine.jdbc.spi.SqlExceptionHelper: SELECT * FROM credentials WHERE token='secret_token'"
        );

        ApiResponse<Object> response = webSocketChatController.handleGeneralException(sensitiveException, dummyMessage);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("An unexpected internal error occurred. Please try again later.");
        assertThat(response.getErrors()).containsExactly("INTERNAL_SERVER_ERROR");
        assertThat(response.getPath()).isEqualTo("/app/chat.send/" + conversation12.getId());

        // Verify sensitive data is NOT leaked
        assertThat(response.getMessage()).doesNotContain("hibernate");
        assertThat(response.getMessage()).doesNotContain("SELECT");
        assertThat(response.getMessage()).doesNotContain("secret_token");
        assertThat(response.getMessage()).doesNotContain("credentials");
    }

    @Test
    @DisplayName("STOMP Errors - Validation exception produces structured validation errors")
    void handleValidationException_ShouldFormatFieldErrors() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/" + conversation12.getId());
        Message<byte[]> dummyMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        SendMessageRequest target = new SendMessageRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "sendMessageRequest");
        bindingResult.addError(new FieldError("sendMessageRequest", "content", "must not be blank"));
        bindingResult.addError(new FieldError("sendMessageRequest", "messageType", "must not be null"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ApiResponse<Object> response = webSocketChatController.handleValidationException(ex, dummyMessage);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getErrors()).containsExactlyInAnyOrder("content: must not be blank", "messageType: must not be null");
        assertThat(response.getPath()).isEqualTo("/app/chat.send/" + conversation12.getId());
    }

    @Test
    @DisplayName("STOMP Errors - AccessDenied and IllegalArgument produce appropriate safe errors")
    void handleAccessDeniedAndIllegalArgument_ShouldReturnDocumentedResponses() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/" + conversation12.getId());
        Message<byte[]> dummyMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        ApiResponse<Object> denied = webSocketChatController.handleAccessDeniedException(
                new AccessDeniedException("Access denied by policy"), dummyMessage
        );
        assertThat(denied.isSuccess()).isFalse();
        assertThat(denied.getMessage()).isEqualTo("You do not have permission to access this resource");
        assertThat(denied.getErrors()).contains("ACCESS_DENIED");

        ApiResponse<Object> illegalArg = webSocketChatController.handleIllegalArgumentException(
                new IllegalArgumentException("Invalid UUID format"), dummyMessage
        );
        assertThat(illegalArg.isSuccess()).isFalse();
        assertThat(illegalArg.getMessage()).isEqualTo("Invalid UUID format");
        assertThat(illegalArg.getErrors()).contains("BAD_REQUEST");
    }

    @Test
    @DisplayName("STOMP - Legitimate message delivery succeeds and broadcasts to conversation topic")
    void handleChatMessage_WhenValidMember_ShouldSucceedAndBroadcast() throws Exception {
        CompletableFuture<Message<?>> broadcastFuture = new CompletableFuture<>();
        List<Message<?>> capturedMessages = new CopyOnWriteArrayList<>();

        MessageHandler handler = m -> {
            capturedMessages.add(m);
            String dest = StompHeaderAccessor.wrap(m).getDestination();
            if (dest != null && dest.equals("/topic/conversations/" + conversation12.getId())) {
                broadcastFuture.complete(m);
            }
        };
        brokerChannel.subscribe(handler);

        try {
            SendMessageRequest validRequest = SendMessageRequest.builder()
                    .content("Hello Bob from Alice!")
                    .messageType(MessageType.TEXT)
                    .build();

            Message<byte[]> message = createStompSendMessage(
                    user1,
                    "/app/chat.send/" + conversation12.getId(),
                    validRequest
            );

            clientInboundChannel.send(message);

            Message<?> broadcastMessage = broadcastFuture.get(5, TimeUnit.SECONDS);
            assertThat(broadcastMessage).as("Expected message to be broadcast to conversation topic").isNotNull();

            // Verify no error message was generated
            boolean hasErrors = capturedMessages.stream()
                    .anyMatch(m -> {
                        String dest = StompHeaderAccessor.wrap(m).getDestination();
                        return dest != null && dest.contains("/queue/errors");
                    });
            assertThat(hasErrors).isFalse();
        } finally {
            brokerChannel.unsubscribe(handler);
        }
    }

    @Test
    @DisplayName("REST - GlobalExceptionHandler remains functional for HTTP endpoints")
    void restExceptionHandling_RemainsUnchanged() throws Exception {
        String token1 = jwtTokenProvider.generateTokenForUser(user1.getId(), user1.getUsername(), user1.getEmail());
        UUID randomId = UUID.randomUUID();

        // Non-member access throws ForbiddenException (403)
        mockMvc.perform(get("/api/v1/conversations/{id}/messages", randomId)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not a participant in this conversation"))
                .andExpect(jsonPath("$.errors[0]").value("FORBIDDEN"));
    }
}
