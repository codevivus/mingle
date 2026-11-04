package com.mingle.match_service.dto;

import com.mingle.match_service.entity.ConversationStatus;

import java.time.Instant;

/**
 * unreadCount counts messages from the other user after the caller last read; otherLastReadMessageId is
 * the read receipt: the highest of the caller's messages the other user has read (0 = none).
 */
public record ConversationResponse(Long id, Long matchId, Long eventId, Long otherUserId,
                                   ConversationStatus status, Instant lockAt, Instant readUntil,
                                   long unreadCount, MessageResponse lastMessage, Long otherLastReadMessageId) {
}
