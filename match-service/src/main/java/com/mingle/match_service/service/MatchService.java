package com.mingle.match_service.service;

import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.client.UserClient;
import com.mingle.match_service.dto.CandidateResponse;
import com.mingle.match_service.dto.MatchResponse;
import com.mingle.match_service.dto.SwipeRequest;
import com.mingle.match_service.dto.SwipeResponse;
import com.mingle.match_service.entity.Match;
import com.mingle.match_service.entity.Swipe;
import com.mingle.match_service.entity.SwipeAction;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.repository.BlockRepository;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private static final int MAX_CANDIDATES = 100;

    private final TicketClient ticketClient;
    private final UserClient userClient;
    private final SwipeRepository swipeRepository;
    private final BlockRepository blockRepository;
    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<CandidateResponse> candidates(Long callerId, Long eventId, int limit) {
        Set<Long> holders = new HashSet<>(ticketClient.holders(eventId));
        if (!holders.contains(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not hold a ticket for this event");
        }
        holders.remove(callerId);
        holders.removeAll(swipeRepository.findSwipedTargets(eventId, callerId));
        holders.removeAll(blockRepository.findBlockedBy(callerId));
        holders.removeAll(blockRepository.findBlockersOf(callerId));
        // A page of the deck at a time, each enriched with the public profile (best effort).
        return holders.stream().sorted().limit(Math.min(Math.max(limit, 1), MAX_CANDIDATES))
                .map(this::withProfile).toList();
    }

    @Transactional
    public SwipeResponse swipe(Long callerId, Long eventId, SwipeRequest request) {
        Long target = request.targetUserId();
        if (callerId.equals(target)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot swipe on yourself");
        }
        Set<Long> holders = new HashSet<>(ticketClient.holders(eventId));
        if (!holders.contains(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not hold a ticket for this event");
        }
        if (blockRepository.existsBetween(callerId, target)) {
            // Same answer whoever blocked whom, so a block cannot be probed.
            throw new ApiException(HttpStatus.NOT_FOUND, "User not available");
        }
        if (!holders.contains(target)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target user does not hold a ticket for this event");
        }

        // Repeating a swipe is a no-op that reports the current outcome.
        if (swipeRepository.findByEventIdAndFromUserAndToUser(eventId, callerId, target).isPresent()) {
            return outcome(eventId, callerId, target);
        }

        swipeRepository.save(Swipe.builder()
                .eventId(eventId).fromUser(callerId).toUser(target).action(request.action()).build());

        if (request.action() == SwipeAction.LIKE
                && swipeRepository.existsByEventIdAndFromUserAndToUserAndAction(eventId, target, callerId, SwipeAction.LIKE)) {
            Long low = Math.min(callerId, target);
            Long high = Math.max(callerId, target);
            if (matchRepository.insertIfAbsent(eventId, low, high) == 1) {
                Match created = matchRepository.findByEventIdAndUserOneAndUserTwo(eventId, low, high).orElseThrow();
                events.publishEvent(new MatchCreatedEvent(created.getId(), eventId, low, high));
            }
        }
        return outcome(eventId, callerId, target);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> matches(Long callerId, Long eventId) {
        List<Match> found = eventId == null
                ? matchRepository.findForUser(callerId)
                : matchRepository.findForUserAndEvent(callerId, eventId);
        return found.stream()
                .map(m -> new MatchResponse(m.getId(), m.getEventId(), m.otherUser(callerId), m.getCreatedAt()))
                .toList();
    }

    private CandidateResponse withProfile(Long userId) {
        try {
            UserClient.Profile p = userClient.findById(userId);
            return new CandidateResponse(userId, p.name(), p.surname(), p.age(), p.city(), p.photoUrl(), p.bio());
        } catch (RuntimeException e) {
            log.warn("Could not load profile of user {}: {}", userId, e.getMessage());
            return CandidateResponse.idOnly(userId);
        }
    }

    private SwipeResponse outcome(Long eventId, Long a, Long b) {
        return matchRepository.findByEventIdAndUserOneAndUserTwo(eventId, Math.min(a, b), Math.max(a, b))
                .filter(Match::isActive)
                .map(m -> new SwipeResponse(true, m.getId()))
                .orElse(new SwipeResponse(false, null));
    }
}
