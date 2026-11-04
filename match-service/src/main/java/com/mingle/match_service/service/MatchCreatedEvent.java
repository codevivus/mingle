package com.mingle.match_service.service;

/** Published inside the swipe transaction when a new match row is inserted. */
public record MatchCreatedEvent(Long matchId, Long eventId, Long userOne, Long userTwo) {
}
