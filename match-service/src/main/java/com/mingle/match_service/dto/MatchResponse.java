package com.mingle.match_service.dto;

import java.time.Instant;

public record MatchResponse(Long id, Long eventId, Long otherUserId, Instant createdAt) {
}
