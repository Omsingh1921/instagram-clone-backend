package com.instagramclone.chat.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.instagramclone.chat.constants.ChatConstants;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.service.interfaces.IChatService;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.common.constants.ApiConstants;
import com.instagramclone.common.exception.ApiException;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.UnauthorizedException;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final IChatService chatService;
    private final IPresenceService presenceService;

    @MessageMapping(ChatConstants.WS_SEND_DESTINATION)
    public void handleChatMessage(
            @DestinationVariable UUID conversationId,
            @Payload(required = false) SendMessageRequest request,
            Principal principal) {
        if (principal == null || !(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorizedException(ChatConstants.ERR_AUTH_REQUIRED_SEND_MESSAGE);
        }
        if (request == null) {
            throw new BadRequestException(ChatConstants.ERR_MESSAGE_REQUEST_NULL);
        }

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chatService.sendMessage(conversationId, request);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @MessageMapping(ChatConstants.WS_TYPING_DESTINATION)
    public void handleTyping(
            @DestinationVariable UUID conversationId,
            @Payload(required = false) Map<String, Object> payload,
            Principal principal) {
        if (principal == null || !(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorizedException(ChatConstants.ERR_AUTH_REQUIRED_TYPING);
        }

        UUID userId = userPrincipal.getId();
        chatService.validateTypingPermission(conversationId, userId);

        boolean isTyping = payload == null || !Boolean.FALSE.equals(payload.get(ChatConstants.FIELD_TYPING));
        presenceService.setTyping(conversationId, userId, isTyping);
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(destinations = ChatConstants.WS_QUEUE_ERRORS, broadcast = false)
    public ApiResponse<Object> handleApiException(ApiException ex, Message<?> message) {
        String destination = extractDestination(message);
        log.warn("STOMP application error [{}] at {}: {}", ex.getErrorCode(), destination, ex.getMessage());
        return ApiResponse.error(ex.getMessage(), ex.getErrorCode(), destination);
    }

    @MessageExceptionHandler({
            org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class
    })
    @SendToUser(destinations = ChatConstants.WS_QUEUE_ERRORS, broadcast = false)
    public ApiResponse<Object> handleValidationException(Exception ex, Message<?> message) {
        String destination = extractDestination(message);
        List<String> errors = new ArrayList<>();
        if (ex instanceof org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException msgEx) {
            for (FieldError fieldError : msgEx.getBindingResult().getFieldErrors()) {
                errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            }
        } else if (ex instanceof org.springframework.web.bind.MethodArgumentNotValidException webEx) {
            for (FieldError fieldError : webEx.getBindingResult().getFieldErrors()) {
                errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            }
        }
        if (errors.isEmpty()) {
            errors.add(ex.getMessage() != null ? ex.getMessage() : "Validation error");
        }
        log.warn("STOMP validation error at {}: {}", destination, errors);
        return ApiResponse.error(ApiConstants.ERR_VALIDATION_FAILED, errors, destination);
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser(destinations = ChatConstants.WS_QUEUE_ERRORS, broadcast = false)
    public ApiResponse<Object> handleAccessDeniedException(AccessDeniedException ex, Message<?> message) {
        String destination = extractDestination(message);
        log.warn("STOMP access denied at {}: {}", destination, ex.getMessage());
        return ApiResponse.error(ApiConstants.ERR_ACCESS_DENIED, ApiConstants.ERR_CODE_ACCESS_DENIED, destination);
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(destinations = ChatConstants.WS_QUEUE_ERRORS, broadcast = false)
    public ApiResponse<Object> handleIllegalArgumentException(IllegalArgumentException ex, Message<?> message) {
        String destination = extractDestination(message);
        log.warn("STOMP invalid argument at {}: {}", destination, ex.getMessage());
        return ApiResponse.error(
                ex.getMessage() != null ? ex.getMessage() : ApiConstants.ERR_INVALID_ARGUMENT,
                ApiConstants.ERR_CODE_BAD_REQUEST,
                destination
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ChatConstants.WS_QUEUE_ERRORS, broadcast = false)
    public ApiResponse<Object> handleGeneralException(Exception ex, Message<?> message) {
        String destination = extractDestination(message);
        log.error("Unhandled STOMP internal error at {}: ", destination, ex);
        return ApiResponse.error(
                ApiConstants.ERR_INTERNAL_ERROR,
                ApiConstants.ERR_CODE_INTERNAL_SERVER_ERROR,
                destination
        );
    }

    private String extractDestination(Message<?> message) {
        if (message == null) {
            return ChatConstants.WS_USER_QUEUE_ERRORS;
        }
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();
        return destination != null ? destination : ChatConstants.WS_USER_QUEUE_ERRORS;
    }
}
