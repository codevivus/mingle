package com.mingle.match_service.service;

import com.mingle.match_service.client.TicketClient;
import com.mingle.match_service.dto.CandidateResponse;
import com.mingle.match_service.dto.MatchResponse;
import com.mingle.match_service.dto.SwipeRequest;
import com.mingle.match_service.dto.SwipeResponse;
import com.mingle.match_service.entity.Match;
import com.mingle.match_service.entity.Swipe;
import com.mingle.match_service.entity.SwipeAction;
import com.mingle.match_service.exception.ApiException;
import com.mingle.match_service.repository.MatchRepository;
import com.mingle.match_service.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final TicketClient ticketClient;
    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<CandidateResponse> candidates(Long callerId, Long eventId) {
        Set<Long> holders = new HashSet<>(ticketClient.holders(eventId));
        if (!holders.contains(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not hold a ticket for this event");
        }
        holders.remove(callerId);
        holders.removeAll(swipeRepository.findSwipedTargets(eventId, callerId));
        return holders.stream().sorted().map(CandidateResponse::new).toList();
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

    private SwipeResponse outcome(Long eventId, Long a, Long b) {
        return matchRepository.findByEventIdAndUserOneAndUserTwo(eventId, Math.min(a, b), Math.max(a, b))
                .map(m -> new SwipeResponse(true, m.getId()))
                .orElse(new SwipeResponse(false, null));
    }
}
