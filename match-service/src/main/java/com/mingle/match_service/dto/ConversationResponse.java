package com.mingle.match_service.dto;

import com.mingle.match_service.entity.ConversationStatus;

import java.time.Instant;

public record ConversationResponse(Long id, Long matchId, Long eventId, Long otherUserId,
                                   ConversationStatus status, Instant lockAt) {
}
