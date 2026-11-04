package com.mingle.match_service.websocket;

import com.mingle.match_service.dto.SendMessageRequest;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.service.ChatMessageDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatMessageDispatcher dispatcher;

    @MessageMapping("/chat/{conversationId}")
    public void send(@DestinationVariable Long conversationId,
                     @Payload SendMessageRequest request,
                     Principal principal) {
        dispatcher.send(Long.valueOf(principal.getName()), conversationId, request.content());
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, Object> onError(ApiException e) {
        return Map.of("status", e.getStatus().value(), "message", e.getMessage());
    }
}
