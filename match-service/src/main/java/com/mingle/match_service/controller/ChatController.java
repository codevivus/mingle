package com.mingle.match_service.controller;

import com.mingle.match_service.dto.ConversationResponse;
import com.mingle.match_service.dto.MessageResponse;
import com.mingle.match_service.dto.SendMessageRequest;
import com.mingle.match_service.service.ChatMessageDispatcher;
import com.mingle.match_service.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String USER_HEADER = "X-User-Id";

    private final ChatService chatService;
    private final ChatMessageDispatcher dispatcher;

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(@RequestHeader(USER_HEADER) Long callerId) {
        return chatService.conversations(callerId);
    }

    @GetMapping("/{id}")
    public ConversationResponse conversation(@RequestHeader(USER_HEADER) Long callerId, @PathVariable Long id) {
        return chatService.conversation(callerId, id);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> history(@RequestHeader(USER_HEADER) Long callerId,
                                         @PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return chatService.history(callerId, id, page, size);
    }

    @PostMapping("/{id}/messages")
    public MessageResponse send(@RequestHeader(USER_HEADER) Long callerId,
                                @PathVariable Long id,
                                @Valid @RequestBody SendMessageRequest request) {
        return dispatcher.send(callerId, id, request.content());
    }

    /** Body is optional: {@code {"upToMessageId": 42}}; without it everything is marked read. */
    @PostMapping("/{id}/read")
    public ConversationResponse markRead(@RequestHeader(USER_HEADER) Long callerId,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) java.util.Map<String, Long> body) {
        return chatService.markRead(callerId, id, body == null ? null : body.get("upToMessageId"));
    }

    @GetMapping("/unread-count")
    public java.util.Map<String, Long> unreadCount(@RequestHeader(USER_HEADER) Long callerId) {
        return java.util.Map.of("count", chatService.unreadTotal(callerId));
    }

    @PostMapping("/{id}/unlock")
    public ConversationResponse unlock(@RequestHeader(USER_HEADER) Long callerId, @PathVariable Long id) {
        return chatService.unlock(callerId, id);
    }
}
