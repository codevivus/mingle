package com.mingle.match_service.websocket;

import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.service.ChatMessageDispatcher;
import com.mingle.match_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

/** A user may only subscribe to the topic of a conversation they take part in. */
@RequiredArgsConstructor
public class SubscriptionAuthInterceptor implements ChannelInterceptor {

    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        if (user == null || destination == null) {
            throw new MessagingException("Unauthenticated subscription");
        }
        if (destination.startsWith("/user/")) {
            return message; // per-user queues such as /user/queue/errors
        }
        if (!destination.startsWith(ChatMessageDispatcher.TOPIC_PREFIX)) {
            throw new MessagingException("Unknown destination " + destination);
        }
        try {
            Long conversationId = Long.valueOf(destination.substring(ChatMessageDispatcher.TOPIC_PREFIX.length()));
            chatService.assertParticipant(Long.valueOf(user.getName()), conversationId);
        } catch (NumberFormatException | ApiException e) {
            throw new MessagingException("Subscription refused: " + e.getMessage());
        }
        return message;
    }
}
