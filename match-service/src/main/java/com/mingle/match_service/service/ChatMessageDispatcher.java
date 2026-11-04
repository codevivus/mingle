package com.mingle.match_service.service;

import com.mingle.match_service.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Saves a message and pushes it to subscribers. Not transactional itself, so the broadcast only
 * happens after {@link ChatService#send} has committed. REST and STOMP senders both go through here.
 */
@Component
@RequiredArgsConstructor
public class ChatMessageDispatcher {

    public static final String TOPIC_PREFIX = "/topic/conversations/";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageResponse send(Long senderId, Long conversationId, String content) {
        MessageResponse saved = chatService.send(senderId, conversationId, content);
        messagingTemplate.convertAndSend(TOPIC_PREFIX + conversationId, saved);
        return saved;
    }
}
