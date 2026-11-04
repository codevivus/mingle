package com.mingle.match_service.dto;

import java.time.Instant;

public record MessageResponse(Long id, Long conversationId, Long senderId, String content, Instant createdAt) {
}
